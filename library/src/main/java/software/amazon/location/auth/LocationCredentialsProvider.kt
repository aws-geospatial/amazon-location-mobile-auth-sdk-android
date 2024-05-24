package software.amazon.location.auth

import android.content.Context
import java.util.Date
import software.amazon.location.auth.data.model.response.Credentials
import software.amazon.location.auth.data.network.AwsRetrofitClient
import software.amazon.location.auth.utils.AwsRegions
import software.amazon.location.auth.utils.AwsRegions.Companion.DEFAULT_REGION
import software.amazon.location.auth.utils.CognitoCredentialsClient
import software.amazon.location.auth.utils.Constants.API_KEY
import software.amazon.location.auth.utils.Constants.BASE_URL
import software.amazon.location.auth.utils.Constants.IDENTITY_POOL_ID
import software.amazon.location.auth.utils.Constants.METHOD
import software.amazon.location.auth.utils.Constants.REGION
import software.amazon.location.auth.utils.Constants.SERVICE_NAME

const val PREFS_NAME = "software.amazon.location.auth"

/**
 * Provides credentials for accessing location-based services through Cognito or API key authentication.
 */
class LocationCredentialsProvider {
    private var context: Context
    private var cognitoCredentialsProvider: CognitoCredentialsProvider? = null
    private var apiKeyProvider: ApiKeyCredentialsProvider? = null
    private var securePreferences: EncryptedSharedPreferences

    /**
     * Initializes with Cognito credentials.
     * @param context The application context.
     * @param identityPoolId The identity pool ID for Cognito authentication.
     * @param region The region for Cognito authentication.
     */
    constructor(context: Context, identityPoolId: String, region: AwsRegions) {
        this.context = context
        securePreferences = EncryptedSharedPreferences(context, PREFS_NAME)
        securePreferences.initEncryptedSharedPreferences()
        securePreferences.put(METHOD, "cognito")
        securePreferences.put(IDENTITY_POOL_ID, identityPoolId)
        securePreferences.put(REGION, region.regionName)
    }

    /**
     * Initializes with an API key.
     * @param context The application context.
     * @param apiKey The API key for authentication.
     */
    constructor(context: Context, apiKey: String) {
        this.context = context
        securePreferences = EncryptedSharedPreferences(context, PREFS_NAME)
        securePreferences.initEncryptedSharedPreferences()
        securePreferences.put(METHOD, "apiKey")
        securePreferences.put(API_KEY, apiKey)
        apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
    }

    /**
     * Initializes with cached credentials.
     * @param context The application context.
     * @throws Exception If API key credentials are not found.
     */
    constructor(context: Context) {
        this.context = context
        securePreferences = EncryptedSharedPreferences(context, PREFS_NAME)
        securePreferences.initEncryptedSharedPreferences()
        val method = securePreferences.get(METHOD)
        if (method === null) throw Exception("No credentials found")
        when (method) {
            "cognito" -> {
                val identityPoolId = securePreferences.get(IDENTITY_POOL_ID)
                val region = securePreferences.get(REGION)
                if (identityPoolId === null || region === null) throw Exception("No credentials found")
                cognitoCredentialsProvider = CognitoCredentialsProvider(context)
            }

            "apiKey" -> {
                val apiKey = securePreferences.get(API_KEY)
                if (apiKey === null) throw Exception("No credentials found")
                apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
            }

            else -> {
                throw Exception("No credentials found")
            }
        }
    }

    /**
     * check AWS credentials.
     *
     * This function retrieves the identity pool ID and region from a secure preferences
     *
     * The function first attempts to initialize the CognitoCredentialsProvider. If it fails
     * or if there are no cached credentials or if the cached credentials are invalid, it
     * generates new credentials.
     *
     * @throws Exception if the identity pool ID or region is not found, or if credential generation fails.
     */
    suspend fun checkCredentials() {
        val identityPoolId = securePreferences.get(IDENTITY_POOL_ID)
        val region = securePreferences.get(REGION)
        if (identityPoolId === null || region === null) throw Exception("No credentials found")
        val isCredentialsAvailable = try {
            cognitoCredentialsProvider = CognitoCredentialsProvider(context)
            true
        } catch (e: Exception) {
            false
        }
        if (!isCredentialsAvailable) {
            generateCredentials(region, identityPoolId)
        } else {
            val credentials = cognitoCredentialsProvider?.getCachedCredentials()
            credentials?.let {
                if (!isCredentialsValid(it)) {
                    AwsRetrofitClient.clearApiService()
                    generateCredentials(region, identityPoolId)
                } else {
                    initAwsRetrofitClient()
                }
            }
        }
    }

    private fun initAwsRetrofitClient() {
        val region = securePreferences.get(REGION) ?: DEFAULT_REGION.regionName
        AwsRetrofitClient.init(getUrl(region), SERVICE_NAME, region, this)
    }

    /**
     * Generates new AWS credentials using the specified region and identity pool ID.
     *
     * This function uses CognitoCredentialsClient to fetch the identity ID and credentials,
     * and then initializes the CognitoCredentialsProvider with the retrieved credentials.
     *
     * @param region The AWS region where the identity pool is located.
     * @param identityPoolId The identity pool ID for Cognito.
     * @throws Exception if the credential generation fails.
     */
    private suspend fun generateCredentials(region: String, identityPoolId: String) {
        val cognitoCredentialsHttpHelper = CognitoCredentialsClient(region)
        try {
            val identityId = cognitoCredentialsHttpHelper.getIdentityId(identityPoolId)
            if (identityId.isNotEmpty()) {
                val credentials = cognitoCredentialsHttpHelper.getCredentials(identityId)
                cognitoCredentialsProvider =
                    CognitoCredentialsProvider(context, credentials.credentials)
                initAwsRetrofitClient()
            }
        } catch (e: Exception) {
            throw Exception("Credentials generation failed")
        }
    }

    /**
     * Checks if the provided credentials are still valid.
     *
     * This function compares the current date with the expiration date of the credentials.
     *
     * @param credentials The AWS credentials to validate.
     * @return True if the credentials are valid (i.e., not expired), false otherwise.
     */
    fun isCredentialsValid(credentials: Credentials): Boolean {
        val expirationTime = credentials.expiration.toLong() * 1000
        val expirationDate = Date(expirationTime)
        val currentDate = Date()
        return currentDate.before(expirationDate)
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
        AwsRetrofitClient.clearApiService()
        cognitoCredentialsProvider?.clearCredentials()
        checkCredentials()
    }

    /**
     * Clears the Cognito credentials.
     * @throws Exception If the Cognito provider is not initialized or if called for API key authentication.
     */
    fun clear() {
        if (cognitoCredentialsProvider === null) throw Exception("Clear is only supported for Cognito credentials. Make sure to use the cognito constructor.")
        cognitoCredentialsProvider?.clearCredentials()
    }

    private fun getUrl(region: String): String {
        val urlBuilder = StringBuilder(BASE_URL.format(region))
        return urlBuilder.toString()
    }
}