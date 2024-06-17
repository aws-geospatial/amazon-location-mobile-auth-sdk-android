package software.amazon.location.auth


import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import software.amazon.location.auth.utils.Constants
import software.amazon.location.auth.utils.Constants.HEADER_HOST
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_CONTENT_SHA256
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_DATE
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_SECURITY_TOKEN
import software.amazon.location.auth.utils.Constants.TIME_PATTERN
import software.amazon.location.auth.utils.HASHING_ALGORITHM
import software.amazon.location.auth.utils.awsAuthorizationHeader

class AwsSignerInterceptor(
    private val serviceName: String,
    private val region: String,
    private val credentialsProvider: LocationCredentialsProvider?
) : Interceptor {

    private val sdfMap = HashMap<String, SimpleDateFormat>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (!originalRequest.url.host.contains("amazonaws.com") || credentialsProvider?.getCredentialsProvider() == null) {
            return chain.proceed(originalRequest)
        }
        runBlocking {
            if (!credentialsProvider.isCredentialsValid()) {
                credentialsProvider.verifyAndRefreshCredentials()
            }
        }
        val accessKeyId = credentialsProvider.getCredentialsProvider()?.accessKeyId
        val secretKey = credentialsProvider.getCredentialsProvider()?.secretKey
        val sessionToken = credentialsProvider.getCredentialsProvider()?.sessionToken
        if (!accessKeyId.isNullOrEmpty() && !secretKey.isNullOrEmpty() && !sessionToken.isNullOrEmpty() && region.isNotEmpty()) {
            val dateMilli = Date().time
            val host = extractHostHeader(originalRequest.url.toString())
            val timeStamp = getTimeStamp(dateMilli)
            val payloadHash = sha256Hex((originalRequest.body ?: "").toString())

            val modifiedRequest =
                originalRequest.newBuilder()
                    .header(HEADER_X_AMZ_DATE, timeStamp)
                    .header(HEADER_HOST, host)
                    .header(HEADER_X_AMZ_SECURITY_TOKEN, sessionToken)
                    .header(HEADER_X_AMZ_CONTENT_SHA256, payloadHash)
                    .build()

            val finalRequest = modifiedRequest.newBuilder()
                .header(
                    Constants.HEADER_AUTHORIZATION,
                    modifiedRequest.awsAuthorizationHeader(
                        accessKeyId,
                        secretKey,
                        region,
                        serviceName,
                        timeStamp
                    )
                )
                .build()
            return chain.proceed(finalRequest)
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
            MessageDigest.getInstance(HASHING_ALGORITHM)
                .digest(data.toByteArray(StandardCharsets.UTF_8))
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
