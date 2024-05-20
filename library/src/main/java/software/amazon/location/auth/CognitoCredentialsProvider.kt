package software.amazon.location.auth

import com.google.gson.Gson
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import software.amazon.location.auth.data.request.GetCredentialRequest
import software.amazon.location.auth.data.request.GetIdentityIdRequest
import software.amazon.location.auth.data.response.GetCredentialResponse
import software.amazon.location.auth.data.response.GetIdentityIdResponse
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_TARGET
import software.amazon.location.auth.utils.Constants.MEDIA_TYPE
import software.amazon.location.auth.utils.Constants.URL


class CognitoCredentialsProvider(private val region: String) {
    var client = OkHttpClient()

    suspend fun getIdentityId(identityPoolId: String): String {
        return suspendCancellableCoroutine { continuation ->
            val requestBody = GetIdentityIdRequest(identityPoolId)
            val mediaType = MEDIA_TYPE.toMediaType()
            val json = Gson().toJson(requestBody)

            val request = Request.Builder()
                .url(getUrl())
                .post(json.toRequestBody(mediaType))
                .addHeader(HEADER_X_AMZ_TARGET, "AWSCognitoIdentityService.GetId")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()
                        val getIdentityIdResponse =
                            Gson().fromJson(jsonResponse, GetIdentityIdResponse::class.java)
                        continuation.resume(getIdentityIdResponse?.identityId ?: "")
                    } else {
                        continuation.resumeWithException(IOException("Failed to get identity ID"))
                    }
                }
            })
        }
    }

    suspend fun getCredentials(identityId: String): GetCredentialResponse {
        return suspendCancellableCoroutine { continuation ->
            val requestBody = GetCredentialRequest(identityId)
            val mediaType = MEDIA_TYPE.toMediaType()
            val json = Gson().toJson(requestBody)

            val request = Request.Builder()
                .url(getUrl())
                .post(json.toRequestBody(mediaType))
                .addHeader(HEADER_X_AMZ_TARGET, "AWSCognitoIdentityService.GetCredentialsForIdentity")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()

                        val getCredentialResponse =
                            Gson().fromJson(jsonResponse, GetCredentialResponse::class.java)
                        continuation.resume(getCredentialResponse ?: throw IOException("Failed to get credentials"))
                    } else {
                        continuation.resumeWithException(IOException("Failed to get credentials"))
                    }
                }
            })
        }
    }

    private fun getUrl(): String {
        val urlBuilder = StringBuilder(URL.format(region))
        return urlBuilder.toString()
    }
}