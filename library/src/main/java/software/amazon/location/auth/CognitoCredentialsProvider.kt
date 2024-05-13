package software.amazon.location.auth

import android.util.Log
import software.amazon.location.auth.data.request.GetCredentialRequest
import software.amazon.location.auth.data.request.GetIdentityIdRequest
import software.amazon.location.auth.data.response.GetCredentialResponse
import software.amazon.location.auth.data.response.GetIdentityIdResponse
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.google.gson.Gson
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response


class CognitoCredentialsProvider(private val region: String, private val awsKeyValueStore: AWSKeyValueStore) {
    private var client = OkHttpClient()
    fun getIdentityId(identityPoolId: String) {
        val url = "https://cognito-identity.$region.amazonaws.com/"
        val requestBody = GetIdentityIdRequest(identityPoolId)
        val mediaType = "application/x-amz-json-1.1".toMediaType()
        val json = Gson().toJson(requestBody)

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(mediaType))
            .addHeader("X-Amz-Target", "AWSCognitoIdentityService.GetId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()

                    val getIdentityIdResponse =
                        Gson().fromJson(jsonResponse, GetIdentityIdResponse::class.java)

                    Log.e("TAG", "getIdentityIdResponse: ${getIdentityIdResponse.identityId}")
                    getIdentityIdResponse?.let { getCredentials(it.identityId) }
                }
            }
        })
    }

    fun getCredentials(identityId: String) {
        val url = "https://cognito-identity.$region.amazonaws.com/"
        val requestBody = GetCredentialRequest(identityId)
        val mediaType = "application/x-amz-json-1.1".toMediaType()
        val json = Gson().toJson(requestBody)

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(mediaType))
            .addHeader("X-Amz-Target", "AWSCognitoIdentityService.GetCredentialsForIdentity")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()

                    val getCredentialResponse =
                        Gson().fromJson(jsonResponse, GetCredentialResponse::class.java)
                    awsKeyValueStore.put("accessKeyId", getCredentialResponse.credentials.accessKeyId)
                    awsKeyValueStore.put("secretKey", getCredentialResponse.credentials.secretKey)
                    awsKeyValueStore.put("sessionToken", getCredentialResponse.credentials.sessionToken)
                    awsKeyValueStore.put("expiration", getCredentialResponse.credentials.expiration.toString())
                    Log.e("TAG", "getCredentialResponse: ${getCredentialResponse.credentials.accessKeyId}")
                }
            }
        })
    }
}