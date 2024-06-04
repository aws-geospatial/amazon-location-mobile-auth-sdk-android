package software.amazon.location.auth

import android.content.Context
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import software.amazon.location.auth.utils.AwsRegions
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
    private var securePreferences: EncryptedSharedPreferences
    private var locationClient: LocationClient? = null
    private var cognitoIdentityClient: CognitoIdentityClient? = null

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
     * Checks AWS credentials availability and validity.
     *
     * This function retrieves the identity pool ID and region from secure preferences. It then
     * attempts to initialize the CognitoCredentialsProvider. If no credentials are found or if
     * the cached credentials are invalid, new credentials are generated.
     *
     * @throws Exception if the identity pool ID or region is not found, or if credential generation fails.
     */
    suspend fun verifyAndRefreshCredentials() {
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
            if (!isCredentialsValid()) {
                generateCredentials(region, identityPoolId)
            }
        }
    }


    /**
     * Retrieves the LocationClient instance with configured AWS credentials.
     *
     * This function initializes and returns the LocationClient with the AWS region and
     * credentials retrieved from secure preferences.
     *
     * @return An instance of LocationClient for interacting with the Amazon Location service.
     * @throws Exception if the AWS region is not found in secure preferences.
     */
    fun getLocationClient(): LocationClient? {
        val identityPoolId = securePreferences.get(IDENTITY_POOL_ID)
        val region = securePreferences.get(REGION)
        if (identityPoolId === null || region === null) throw Exception("No credentials found")
        if (locationClient == null) {
            val credentialsProvider = createCredentialsProvider()
            locationClient = generateLocationClient(region, credentialsProvider)
        }
        return locationClient
    }

    /**
     * Generates a new instance of LocationClient with the specified region and credentials provider.
     *
     * @param region The AWS region for the LocationClient.
     * @param credentialsProvider The credentials provider for the LocationClient.
     * @return A new instance of LocationClient.
     */
    fun generateLocationClient(
        region: String?,
        credentialsProvider: CredentialsProvider
    ): LocationClient {
        return LocationClient {
            this.region = region
            this.credentialsProvider = credentialsProvider
        }
    }

    /**
     * Creates a new instance of CredentialsProvider using the credentials obtained from the current provider.
     *
     * This function constructs a CredentialsProvider with the AWS credentials retrieved from the existing provider.
     * It extracts the access key ID, secret access key, and session token from the current provider and initializes
     * a StaticCredentialsProvider with these credentials.
     *
     * @return A new instance of CredentialsProvider initialized with the current AWS credentials.
     * @throws Exception if credentials cannot be retrieved.
     */
    private fun createCredentialsProvider(): CredentialsProvider {
        if (getCredentialsProvider() == null || getCredentialsProvider()?.accessKeyId == null || getCredentialsProvider()?.secretKey == null) throw Exception(
            "Failed to get credentials"
        )
        return StaticCredentialsProvider(
            Credentials.invoke(
                accessKeyId = getCredentialsProvider()?.accessKeyId!!,
                secretAccessKey = getCredentialsProvider()?.secretKey!!,
                sessionToken = getCredentialsProvider()?.sessionToken,
            )
        )
    }

    /**
     * Generates new AWS credentials using the specified region and identity pool ID.
     *
     * This function fetches the identity ID and credentials from Cognito, and then initializes
     * the CognitoCredentialsProvider with the retrieved credentials.
     *
     * @param region The AWS region where the identity pool is located.
     * @param identityPoolId The identity pool ID for Cognito.
     * @throws Exception if the credential generation fails.
     */
    private suspend fun generateCredentials(region: String, identityPoolId: String) {
        if (cognitoIdentityClient == null) {
            cognitoIdentityClient = generateCognitoIdentityClient(region)
        }
        try {
            val getIdResponse = cognitoIdentityClient?.getId(GetIdRequest { this.identityPoolId = identityPoolId })
            val identityId =
                getIdResponse?.identityId ?: throw Exception("Failed to get identity ID")
            if (identityId.isNotEmpty()) {
                val getCredentialsResponse =
                    cognitoIdentityClient?.getCredentialsForIdentity(GetCredentialsForIdentityRequest {
                        this.identityId = identityId
                    })

                val credentials = getCredentialsResponse?.credentials
                    ?: throw Exception("Failed to get credentials")
                if (credentials.accessKeyId == null || credentials.secretKey == null || credentials.sessionToken == null) throw Exception(
                    "Credentials generation failed"
                )
                cognitoCredentialsProvider = CognitoCredentialsProvider(
                    context,
                    credentials
                )
                locationClient = null
            }
        } catch (e: Exception) {
            throw Exception("Credentials generation failed")
        }
    }

    /**
     * Generates a new instance of CognitoIdentityClient with the specified region.
     *
     * @param region The AWS region for the CognitoIdentityClient.
     * @return A new instance of CognitoIdentityClient.
     */

    fun generateCognitoIdentityClient(region: String): CognitoIdentityClient {
        return CognitoIdentityClient { this.region = region }
    }

    /**
     * Checks if the provided credentials are still valid.
     *
     * @return True if the credentials are valid (i.e., not expired), false otherwise.
     */
    fun isCredentialsValid(): Boolean {
        val credentials = cognitoCredentialsProvider?.getCachedCredentials()
        val currentTimeMillis = Instant.now().epochMilliseconds
        val expirationTimeMillis = credentials?.expiration?.epochMilliseconds ?: throw Exception("Failed to get credentials")
        return currentTimeMillis < expirationTimeMillis
    }

    /**
     * Retrieves the Cognito credentials.
     * @return The Credentials instance.
     * @throws Exception If the Cognito provider is not initialized.
     */
    fun getCredentialsProvider(): aws.sdk.kotlin.services.cognitoidentity.model.Credentials? {
        if (cognitoCredentialsProvider === null) throw Exception("Cognito credentials not initialized")
        return cognitoCredentialsProvider?.getCachedCredentials()
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
        locationClient = null
        cognitoCredentialsProvider?.clearCredentials()
        verifyAndRefreshCredentials()
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