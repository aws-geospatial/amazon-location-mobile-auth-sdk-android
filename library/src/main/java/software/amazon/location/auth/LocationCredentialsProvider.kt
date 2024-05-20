package software.amazon.location.auth

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import software.amazon.location.auth.data.response.Credentials
import software.amazon.location.auth.utils.AwsRegions
import software.amazon.location.auth.utils.CognitoCredentialsClient
import software.amazon.location.auth.utils.Constants.API_KEY
import software.amazon.location.auth.utils.Constants.IDENTITY_POOL_ID
import software.amazon.location.auth.utils.Constants.METHOD
import software.amazon.location.auth.utils.Constants.REGION

const val PREFS_NAME = "software.amazon.location.auth"

/**
 * Provides credentials for accessing location-based services through Cognito or API key authentication.
 */
class LocationCredentialsProvider {
    private var context: Context
    private var cognitoCredentialsProvider: CognitoCredentialsProvider? = null
    private var apiKeyProvider: ApiKeyCredentialsProvider? = null
    private var awsKeyValueStore: AWSKeyValueStore

    /**
     * Initializes with Cognito credentials.
     * @param context The application context.
     * @param identityPoolId The identity pool ID for Cognito authentication.
     * @param region The region for Cognito authentication.
     */
    constructor(context: Context, identityPoolId: String, region: AwsRegions) {
        this.context = context
        awsKeyValueStore = AWSKeyValueStore(context, PREFS_NAME, true)
        awsKeyValueStore.put(METHOD, "cognito")
        awsKeyValueStore.put(IDENTITY_POOL_ID, identityPoolId)
        awsKeyValueStore.put(REGION, region.regionName)
    }

    /**
     * Initializes with an API key.
     * @param context The application context.
     * @param apiKey The API key for authentication.
     */
    constructor(context: Context, apiKey: String) {
        this.context = context
        awsKeyValueStore = AWSKeyValueStore(context, PREFS_NAME, true)
        awsKeyValueStore.put(METHOD, "apiKey")
        awsKeyValueStore.put(API_KEY, apiKey)
        apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
    }

    /**
     * Initializes with cached credentials.
     * @param context The application context.
     * @throws Exception If API key credentials are not found.
     */
    constructor(context: Context) {
        this.context = context
        awsKeyValueStore = AWSKeyValueStore(context, PREFS_NAME, true)
        val method = awsKeyValueStore.get(METHOD)
        if (method === null) throw Exception("No credentials found")
        when (method) {
            "cognito" -> {
                val identityPoolId = awsKeyValueStore.get(IDENTITY_POOL_ID)
                val region = awsKeyValueStore.get(REGION)
                if (identityPoolId === null || region === null) throw Exception("No credentials found")
                cognitoCredentialsProvider = CognitoCredentialsProvider(context)
            }

            "apiKey" -> {
                val apiKey = awsKeyValueStore.get(API_KEY)
                if (apiKey === null) throw Exception("No credentials found")
                apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
            }

            else -> {
                throw Exception("No credentials found")
            }
        }
    }

    /**
     * Generates and stores AWS credentials.
     *
     * This function fetches the identity pool ID and region from the key-value store,
     * uses these to retrieve an identity ID and credentials from AWS Cognito, and then
     * stores these credentials using a CognitoCredentialsProvider.
     *
     * @return True if the credentials were successfully generated and stored, false otherwise.
     */
    suspend fun generateCredentials() {
        val identityPoolId = awsKeyValueStore.get(IDENTITY_POOL_ID)
        val region = awsKeyValueStore.get(REGION)
        if (identityPoolId === null || region === null) throw Exception("No credentials found")
        val cognitoCredentialsHttpHelper = CognitoCredentialsClient(region)
        try {
            val identityId = cognitoCredentialsHttpHelper.getIdentityId(identityPoolId)
            if (identityId.isNotEmpty()) {
                val credentials = cognitoCredentialsHttpHelper.getCredentials(identityId)
                cognitoCredentialsProvider = CognitoCredentialsProvider(context, credentials.credentials)
            }
        } catch (e: Exception) {
            throw Exception("Credentials generation failed")
        }
    }

    /**
     * Retrieves the Cognito credentials.
     * @return The Credentials instance.
     * @throws Exception If the Cognito provider is not initialized.
     */
    fun getCredentialsProvider(): Credentials {
        if (cognitoCredentialsProvider === null) throw Exception("Cognito credentials not initialized")
        return cognitoCredentialsProvider?.getCachedCredentials()!!
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
    suspend fun refresh() {
        if (cognitoCredentialsProvider === null) throw Exception("Refresh is only supported for Cognito credentials. Make sure to use the cognito constructor.")
        cognitoCredentialsProvider?.clearCredentials()
        generateCredentials()
    }

    /**
     * Clears the Cognito credentials.
     * @throws Exception If the Cognito provider is not initialized or if called for API key authentication.
     */
    fun clear() {
        if (cognitoCredentialsProvider === null) throw Exception("Clear is only supported for Cognito credentials. Make sure to use the cognito constructor.")
        cognitoCredentialsProvider?.clearCredentials()
    }
}