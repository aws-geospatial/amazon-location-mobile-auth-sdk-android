// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
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
import software.amazon.location.auth.utils.Constants.QUERY_PARAM_KEY
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
        val credentials: Credentials?
        runBlocking {
            credentials = credentialsProvider?.getCredentials()
        }
        if (!originalRequest.url.host.contains("amazonaws.com") || credentials == null) {
            return chain.proceed(originalRequest)
        }
        val method = credentialsProvider?.getMethod()
        if (method === null) throw Exception("No credentials found")
        if (method == "apiKey") {
            val originalHttpUrl = originalRequest.url
            val hasKey = originalHttpUrl.queryParameter(QUERY_PARAM_KEY) != null
            val newHttpUrl = if (!hasKey) {
                val apiKey = credentialsProvider?.getApiKey()
                if (!apiKey.isNullOrEmpty()) {
                    originalHttpUrl.newBuilder()
                        .addQueryParameter(QUERY_PARAM_KEY, apiKey)
                        .build()
                }
                else {
                    originalHttpUrl
                }
            } else {
                originalHttpUrl
            }
            val newRequest = originalRequest.newBuilder()
                .url(newHttpUrl)
                .build()

            return chain.proceed(newRequest)
        } else {
            val accessKeyId = credentials.accessKeyId
            val secretKey = credentials.secretAccessKey
            val sessionToken = credentials.sessionToken
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
