package software.amazon.location.auth


import android.content.Context
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okhttp3.Interceptor
import okhttp3.Response
import software.amazon.location.auth.data.response.Credentials
import software.amazon.location.auth.utils.Constants.HEADER_HOST
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_CONTENT_SHA256
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_DATE
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_SECURITY_TOKEN
import software.amazon.location.auth.utils.Constants.REGION
import software.amazon.location.auth.utils.Constants.TIME_PATTERN
import software.amazon.location.auth.utils.signed

class AwsSignerInterceptor(
    private val context: Context,
    private val serviceName: String,
    private val credentialsProvider: Credentials?
) : Interceptor {

    private lateinit var securePreferences: EncryptedSharedPreferences
    private val sdfMap = HashMap<String, SimpleDateFormat>()

    override fun intercept(chain: Interceptor.Chain): Response {
        securePreferences = EncryptedSharedPreferences(context, PREFS_NAME)
        securePreferences.initEncryptedSharedPreferences()
        val accessKeyId = credentialsProvider?.accessKeyId
        val secretKey = credentialsProvider?.secretKey
        val sessionToken = credentialsProvider?.sessionToken
        val region = securePreferences.get(REGION)
        val originalRequest = chain.request()
        if (!accessKeyId.isNullOrEmpty() && !secretKey.isNullOrEmpty() && !sessionToken.isNullOrEmpty() && !region.isNullOrEmpty() && originalRequest.url.host.contains("amazonaws.com")) {
            val dateMilli = Date().time
            val host = extractHostHeader(originalRequest.url.toString())
            val timeStamp = getTimeStamp(dateMilli)
            val payloadHash = sha256Hex((originalRequest.body ?: "").toString())

            val modifiedRequestWithoutAuthorization =
                originalRequest.newBuilder()
                    .header(HEADER_X_AMZ_DATE, timeStamp)
                    .header(HEADER_HOST, host)
                    .header(HEADER_X_AMZ_SECURITY_TOKEN, sessionToken)
                    .header(HEADER_X_AMZ_CONTENT_SHA256, payloadHash)
                    .build()

            val modifiedRequest =modifiedRequestWithoutAuthorization.signed(accessKeyId, secretKey, region, serviceName)
            return chain.proceed(modifiedRequest)
        }
        return chain.proceed(originalRequest)
    }

    private fun extractHostHeader(urlString: String): String {
        val url = URL(urlString)
        return url.host
    }

    private fun getTimeStamp(dateMilli: Long): String {
        return format(
            Date(dateMilli)
        )
    }

    private fun getSimpleDateFormat(): SimpleDateFormat {
        return sdfMap.getOrPut(TIME_PATTERN) {
            SimpleDateFormat(TIME_PATTERN, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
        }
    }

    private fun format(date: Date): String {
        return getSimpleDateFormat().format(date)
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
