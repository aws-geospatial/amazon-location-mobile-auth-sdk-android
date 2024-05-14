package software.amazon.location.auth

import android.content.Context
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.amazonaws.regions.Regions

const val PREFS_NAME = "software.amazon.location.auth"

/**
 * Provides credentials for accessing location-based services through Cognito or API key authentication.
 */
class LocationCredentialsProvider {
    private var cognitoProvider: CognitoCachingCredentialsProvider? = null
    private var apiKeyProvider: ApiKeyCredentialsProvider? = null
    private var awsKeyValueStore: AWSKeyValueStore

    /**
     * Initializes with Cognito credentials.
     * @param context The application context.
     * @param identityPoolId The identity pool ID for Cognito authentication.
     * @param region The region for Cognito authentication.
     */
    constructor(context: Context, identityPoolId: String, region: Regions) {
        awsKeyValueStore = AWSKeyValueStore(context, PREFS_NAME, true)
        val cognitoCachingCredentialsProvider = CognitoCachingCredentialsProvider(
            context,
            identityPoolId,
            region
        )
        cognitoCachingCredentialsProvider.sessionDuration = 3600
        cognitoCachingCredentialsProvider.refreshThreshold = 60
        cognitoProvider = cognitoCachingCredentialsProvider
        awsKeyValueStore.put("method", "cognito")
        awsKeyValueStore.put("identityPoolId", identityPoolId)
        awsKeyValueStore.put("region", region.getName())
    }

    /**
     * Initializes with an API key.
     * @param context The application context.
     * @param apiKey The API key for authentication.
     */
    constructor(context: Context, apiKey: String) {
        awsKeyValueStore = AWSKeyValueStore(context, PREFS_NAME, true)
        awsKeyValueStore.put("method", "apiKey")
        awsKeyValueStore.put("apiKey", apiKey)
        apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
    }

    /**
     * Initializes with cached credentials.
     * @param context The application context.
     * @throws Exception If API key credentials are not found.
     */
    constructor(context: Context) {
        awsKeyValueStore = AWSKeyValueStore(context, PREFS_NAME, true)
        val method = awsKeyValueStore.get("method")
        if (method === null) throw Exception("No credentials found")
        when (method) {
            "cognito" -> {
                val identityPoolId = awsKeyValueStore.get("identityPoolId")
                val region = awsKeyValueStore.get("region")
                if (identityPoolId === null || region === null) throw Exception("No credentials found")
                cognitoProvider = CognitoCachingCredentialsProvider(
                    context,
                    identityPoolId,
                    Regions.fromName(region)
                )
            }

            "apiKey" -> {
                val apiKey = awsKeyValueStore.get("apiKey")
                if (apiKey === null) throw Exception("No credentials found")
                apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
            }

            else -> {
                throw Exception("No credentials found")
            }
        }
    }

    suspend fun generateCredentials(identityPoolId: String, region: Regions): Boolean {
        val cognitoCredentialsProvider = CognitoCredentialsProvider(region.getName())
        try {
            val identityId = cognitoCredentialsProvider.getIdentityId(identityPoolId)
            if (identityId.isNotEmpty()) {
                val credentials = cognitoCredentialsProvider.getCredentials(identityId)
                awsKeyValueStore.put("accessKeyId", credentials.credentials.accessKeyId)
                awsKeyValueStore.put("secretKey", credentials.credentials.secretKey)
                awsKeyValueStore.put("sessionToken", credentials.credentials.sessionToken)
                awsKeyValueStore.put("expiration", credentials.credentials.expiration.toString())
                return true
            }
        } catch (e: Exception) {
           e.printStackTrace()
        }
        return false
    }

    /**
     * Retrieves the Cognito credentials provider.
     * @return The CognitoCachingCredentialsProvider instance.
     * @throws Exception If the Cognito provider is not initialized.
     */
    fun getCredentialsProvider(): AWSCredentialsProvider {
        if (cognitoProvider === null) throw Exception("Cognito credentials not initialized")
        return cognitoProvider!!
    }

    /**
     * Retrieves the API key credentials provider.
     * @return The ApiKeyCredentialsProvider instance.
     * @throws Exception If the API key provider is not initialized.
     */
    fun getApiKeyProvider(): ApiKeyCredentialsProvider {
        if (apiKeyProvider === null) throw Exception("Api key provider not initialized")
        return apiKeyProvider!!
    }

    /**
     * Refreshes the Cognito credentials.
     * @throws Exception If the Cognito provider is not initialized or if called for API key authentication.
     */
    fun refresh() {
        if (cognitoProvider === null) throw Exception("Refresh is only supported for Cognito credentials. Make sure to use the cognito constructor.")
        cognitoProvider!!.refresh()
    }

    /**
     * Clears the Cognito credentials.
     * @throws Exception If the Cognito provider is not initialized or if called for API key authentication.
     */
    fun clear() {
        if (cognitoProvider === null) throw Exception("Clear is only supported for Cognito credentials. Make sure to use the cognito constructor.")
        cognitoProvider!!.clear()
    }
}