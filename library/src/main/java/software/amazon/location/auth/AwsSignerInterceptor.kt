package software.amazon.location.auth


import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

open class AwsSignerInterceptor(
    private val context: Context,
    private val serviceName: String
) : Interceptor {

    private lateinit var awsKeyValueStore: AWSKeyValueStore
    private val algorithm: String = "AWS4-HMAC-SHA256"
    private val timePattern = "yyyyMMdd'T'HHmmss'Z'"
    private val datePattern = "yyyyMMdd"
    private val sdfMap = HashMap<String, SimpleDateFormat>()
    private val terminator: String = "aws4_request"

    override fun intercept(chain: Interceptor.Chain): Response {
        awsKeyValueStore = AWSKeyValueStore(context, PREFS_NAME, true)
        val accessKeyId = awsKeyValueStore.get("accessKeyId")
        val secretKey = awsKeyValueStore.get("secretKey")
        val sessionToken = awsKeyValueStore.get("sessionToken")
        val region = awsKeyValueStore.get("region")
        val originalRequest = chain.request()
        if (originalRequest.url.host.contains("amazonaws.com")) {
            val dateMilli = Date().time
            val dateStamp: String = getDateStamp(dateMilli)
            val scope = getScope(dateStamp, region)
            val signingCredentials = "$accessKeyId/$scope"
            val host = extractHostHeader(originalRequest.url.toString())
            val timeStamp = getTimeStamp(dateMilli)
            val payloadHash = sha256Hex((originalRequest.body ?: "").toString())

            val modifiedRequestWithoutAuthorization =
                originalRequest.newBuilder()
                    .header("X-Amz-Date", timeStamp)
                    .header("Host", host)
                    .header("x-amz-security-token", sessionToken)
                    .tag(null)
                    .build()

            var canonicalHeaders = ""
            val headers = modifiedRequestWithoutAuthorization.headers
            headers.forEach {
                if (it.first != "User-Agent") {
                    canonicalHeaders += it.first.lowercase() + ":" + it.second + "\n"
                }
            }
            val canonicalRequest =
                "${modifiedRequestWithoutAuthorization.method} \n${modifiedRequestWithoutAuthorization.url.encodedPath} \n${modifiedRequestWithoutAuthorization.url.query ?: ""} \n$canonicalHeaders \n${
                    getSignedHeadersString(
                        modifiedRequestWithoutAuthorization
                    )
                } \n$payloadHash"
            val hashedCanonicalRequest = sha256Hex(canonicalRequest)
            val stringToSign =
                algorithm + "\n" + dateStamp + "\n" + scope + "\n" + hashedCanonicalRequest
            val signingKey = getSignatureKey(secretKey, dateStamp, region, serviceName)

            val signature = hmacSha256Hex(signingKey, stringToSign)
            val credentialsAuthorizationHeader =
                "Credential=$signingCredentials"
            val signedHeadersAuthorizationHeader =
                "SignedHeaders=" + getSignedHeadersString(modifiedRequestWithoutAuthorization)
            val signatureAuthorizationHeader =
                "Signature=$signature"
            val authorizationHeader: String = (algorithm + " "
                    + credentialsAuthorizationHeader + ", "
                    + signedHeadersAuthorizationHeader + ", "
                    + signatureAuthorizationHeader)
            val modifiedRequest =
                modifiedRequestWithoutAuthorization.newBuilder()
                    .header("Authorization", authorizationHeader)
                    .build()
            return chain.proceed(modifiedRequest)
        }
        return chain.proceed(originalRequest)
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun getSignatureKey(
        key: String,
        dateStamp: String,
        regionName: String,
        serviceName: String
    ): ByteArray {
        val kSecret = "AWS4$key".toByteArray(StandardCharsets.UTF_8)
        val kDate: ByteArray = hmacSha256(kSecret, dateStamp)
        val kRegion: ByteArray = hmacSha256(kDate, regionName)
        val kService: ByteArray = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun hmacSha256Hex(key: ByteArray, data: String): String {
        return bytesToHex(hmacSha256(key, data))
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Error: HmacSHA256 algorithm not available", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Error: Invalid key for HmacSHA256", e)
        }
    }

    private fun getSignedHeadersString(request: Request): String {
        val sortedHeaders: MutableList<String> = ArrayList()
        sortedHeaders.addAll(request.headers.names())
        Collections.sort(sortedHeaders, java.lang.String.CASE_INSENSITIVE_ORDER)

        val buffer = StringBuilder()
        for (header in sortedHeaders) {
            if (header != "User-Agent") {
                if (buffer.isNotEmpty()) {
                    buffer.append(";")
                }
                buffer.append(header.lowercase())
            }
        }

        return buffer.toString()
    }

    private fun getScope(dateStamp: String, region: String): String {
        val scope = "$dateStamp/$region/$serviceName/$terminator"
        return scope
    }

    private fun extractHostHeader(urlString: String): String {
        val url = URL(urlString)
        return url.host
    }

    private fun getDateStamp(dateMilli: Long): String {
        return format(
            datePattern,
            Date(dateMilli)
        )
    }

    private fun getTimeStamp(dateMilli: Long): String {
        return format(
            timePattern,
            Date(dateMilli)
        )
    }

    private fun getSimpleDateFormat(pattern: String): SimpleDateFormat {
        return sdfMap.getOrPut(pattern) {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
        }
    }

    private fun format(pattern: String, date: Date): String {
        return getSimpleDateFormat(pattern).format(date)
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun sha256Hex(data: String): String {
        return bytesToHex(
            MessageDigest.getInstance("SHA-256").digest(data.toByteArray(StandardCharsets.UTF_8))
        )
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val result = java.lang.StringBuilder()
        for (b in bytes) {
            result.append(String.format("%02x", b))
        }
        return result.toString()
    }
}
