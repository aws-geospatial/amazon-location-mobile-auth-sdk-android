package software.amazon.location.auth

import android.content.Context
import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import aws.sdk.kotlin.services.geomaps.GeoMapsClient
import aws.sdk.kotlin.services.geoplaces.GeoPlacesClient
import aws.sdk.kotlin.services.georoutes.GeoRoutesClient
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.HttpException
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import software.amazon.location.auth.utils.AwsRegions
import software.amazon.location.auth.utils.Constants.API_KEY
import software.amazon.location.auth.utils.Constants.IDENTITY_POOL_ID
import software.amazon.location.auth.utils.Constants.METHOD
import software.amazon.location.auth.utils.Constants.REGION

const val PREFS_NAME = "software.amazon.location.auth"

/**
 * Provides credentials for accessing location-based services through Cognito authentication.
 */
class LocationCredentialsProvider {
    private var credentialsProvider: CredentialsProvider? = null
    private var customCredentials: aws.sdk.kotlin.services.cognitoidentity.model.Credentials? = null
    private var emptyCredentials: aws.sdk.kotlin.services.cognitoidentity.model.Credentials? = null
    private var context: Context
    private var cognitoCredentialsProvider: CognitoCredentialsProvider? = null
    private var securePreferences: EncryptedSharedPreferences
    private var locationClient: LocationClient? = null
    private var geoMapsClient: GeoMapsClient? = null
    private var geoPlacesClient: GeoPlacesClient? = null
    private var geoRoutesClient: GeoRoutesClient? = null
    private var cognitoIdentityClient: CognitoIdentityClient? = null
    private var apiKeyProvider: ApiKeyCredentialsProvider? = null

    /**
     * Initializes with Cognito credentials.
     * @param context The application context.
     * @param identityPoolId The identity pool ID for Cognito authentication.
     * @param region The region for Cognito authentication.
     */
    constructor(context: Context, identityPoolId: String, region: AwsRegions) {
        this.context = context
        securePreferences = initPreference(context)
        securePreferences.put(METHOD, "cognito")
        securePreferences.put(IDENTITY_POOL_ID, identityPoolId)
        securePreferences.put(REGION, region.regionName)
    }

    /**
     * Initializes with region.
     * @param context The application context.
     * @param region The region for Cognito authentication.
     */
    constructor(context: Context, region: AwsRegions) {
        this.context = context
        securePreferences = initPreference(context)
        securePreferences.put(METHOD, "custom")
        securePreferences.put(REGION, region.regionName)
    }

    /**
     * Initializes with an API key.
     * @param context The application context.
     * @param region The region for Cognito authentication.
     * @param apiKey The API key for authentication.
     */
    constructor(context: Context, region: AwsRegions, apiKey: String) {
        this.context = context
        securePreferences = initPreference(context)
        securePreferences.put(METHOD, "apiKey")
        securePreferences.put(API_KEY, apiKey)
        securePreferences.put(REGION, region.regionName)
        apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
    }


    /**
     * Initializes with cached credentials.
     * @param context The application context.
     * @throws Exception If credentials are not found.
     */
    constructor(context: Context) {
        this.context = context
        securePreferences = initPreference(context)
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
                val region = securePreferences.get(REGION)
                if (apiKey === null || region === null) throw Exception("No credentials found")
                apiKeyProvider = ApiKeyCredentialsProvider(context, apiKey)
            }
            else -> {
                throw Exception("No credentials found")
            }
        }
    }

    fun initPreference(context: Context): EncryptedSharedPreferences {
        return EncryptedSharedPreferences(context, PREFS_NAME).apply { initEncryptedSharedPreferences() }
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
     * Initializes the Location client with custom CredentialsProvider
     *
     * This method generates a Cognito identity client if not already present, retrieves an identity ID using the
     * provided identity pool ID, and initializes the `CognitoCredentialsProvider` with the resolved credentials.
     *
     * @param credentialsProvider The provider for AWS credentials.
     */
    suspend fun initializeLocationClient(credentialsProvider: CredentialsProvider) {
        val region = securePreferences.get(REGION)
        if (region === null) throw Exception("No credentials found")

        this.credentialsProvider = credentialsProvider
        setCustomCredentials(credentialsProvider, region)
    }

    /**
     * Sets custom credentials using the provided credentials provider and region.
     *
     * @param credentialsProvider The provider for AWS credentials, which is used to resolve AWS access credentials.
     * @param region The AWS region to be used for initializing the location client.
     */
    private suspend fun setCustomCredentials(credentialsProvider: CredentialsProvider, region: String) {
        val credentials = credentialsProvider.resolve()
        customCredentials = aws.sdk.kotlin.services.cognitoidentity.model.Credentials.invoke {
            accessKeyId = credentials.accessKeyId
            secretKey = credentials.secretAccessKey
            sessionToken = credentials.sessionToken
            expiration = credentials.expiration
        }
        locationClient = generateLocationClient(region, credentialsProvider)
        geoMapsClient = generateGeoMapsClient(region, credentialsProvider)
        geoPlacesClient = generateGeoPlacesClient(region, credentialsProvider)
        geoRoutesClient = generateGeoRoutesClient(region, credentialsProvider)
    }

    /**
     * Initializes the AWS Location Service client using API key.
     *
     * This function retrieves the API key and region from `securePreferences`,
     * creates an empty credentials provider, and generates the `locationClient`
     * with the provided region and API key.
     *
     * @throws Exception if the API key or region is not found in `securePreferences`.
     */
    suspend fun initializeLocationClient() {
        val apiKey = securePreferences.get(API_KEY)
        val region = securePreferences.get(REGION)
        if (apiKey === null || region === null) throw Exception("No credentials found")

        val credentials = createEmptyCredentialsProvider().resolve()
        emptyCredentials = aws.sdk.kotlin.services.cognitoidentity.model.Credentials.invoke {
            accessKeyId = credentials.accessKeyId
            secretKey = credentials.secretAccessKey
            sessionToken = credentials.sessionToken
        }
        geoMapsClient = generateGeoMapsClient(region, createEmptyCredentialsProvider(), apiKey)
        locationClient = generateLocationClient(region, createEmptyCredentialsProvider(), apiKey)
        geoPlacesClient = generateGeoPlacesClient(region, createEmptyCredentialsProvider(), apiKey)
        geoRoutesClient = generateGeoRoutesClient(region, createEmptyCredentialsProvider(), apiKey)
    }

    /**
     * Creates an empty `StaticCredentialsProvider` with no access keys or secret keys.
     *
     * This is useful for bypassing the default credentials provider chain when
     * credentials are not yet available or are not required for certain operations.
     *
     * @return A `StaticCredentialsProvider` instance with empty credentials.
     */
    private fun createEmptyCredentialsProvider(): CredentialsProvider =
        StaticCredentialsProvider(
            Credentials.invoke(
                accessKeyId = "",
                secretAccessKey = "",
                sessionToken = null,
            ),
        )

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
        val region = securePreferences.get(REGION) ?: throw Exception("No credentials found")

        if (locationClient == null) {
            val method = securePreferences.get(METHOD) ?: throw Exception("No credentials found")
            locationClient = when (method) {
                "apiKey" -> {
                    val apiKey = securePreferences.get(API_KEY) ?: throw Exception("API key not found")
                    generateLocationClient(region, createEmptyCredentialsProvider(), apiKey)
                }
                else -> {
                    val credentialsProvider = createCredentialsProvider()
                    generateLocationClient(region, credentialsProvider)
                }
            }
        }
        return locationClient
    }

    /**
     * Retrieves the GeoMapsClient instance with configured AWS credentials.
     *
     * This function initializes and returns the GeoMapsClient with the AWS region and
     * credentials retrieved from secure preferences.
     *
     * @return An instance of GeoMapsClient for interacting with the Amazon Location service.
     * @throws Exception if the AWS region is not found in secure preferences.
     */
    fun getGeoMapsClient(): GeoMapsClient? {
        val region = securePreferences.get(REGION) ?: throw Exception("No credentials found")

        if (geoMapsClient == null) {
            val method = securePreferences.get(METHOD) ?: throw Exception("No credentials found")
            geoMapsClient = when (method) {
                "apiKey" -> {
                    val apiKey = securePreferences.get(API_KEY) ?: throw Exception("API key not found")
                    generateGeoMapsClient(region, createEmptyCredentialsProvider(), apiKey)
                }
                else -> {
                    val credentialsProvider = createCredentialsProvider()
                    generateGeoMapsClient(region, credentialsProvider)
                }
            }
        }
        return geoMapsClient
    }

    /**
     * Retrieves the GeoPlacesClient instance with configured AWS credentials.
     *
     * This function initializes and returns the GeoPlacesClient with the AWS region and
     * credentials retrieved from secure preferences.
     *
     * @return An instance of GeoPlacesClient for interacting with the Amazon Location service.
     * @throws Exception if the AWS region is not found in secure preferences.
     */
    fun getGeoPlacesClient(): GeoPlacesClient? {
        val region = securePreferences.get(REGION) ?: throw Exception("No credentials found")

        if (geoPlacesClient == null) {
            val method = securePreferences.get(METHOD) ?: throw Exception("No credentials found")
            geoPlacesClient = when (method) {
                "apiKey" -> {
                    val apiKey = securePreferences.get(API_KEY) ?: throw Exception("API key not found")
                    generateGeoPlacesClient(region, createEmptyCredentialsProvider(), apiKey)
                }
                else -> {
                    val credentialsProvider = createCredentialsProvider()
                    generateGeoPlacesClient(region, credentialsProvider)
                }
            }
        }
        return geoPlacesClient
    }


    /**
     * Retrieves the GeoRoutesClient instance with configured AWS credentials.
     *
     * This function initializes and returns the GeoRoutesClient with the AWS region and
     * credentials retrieved from secure preferences.
     *
     * @return An instance of GeoRoutesClient for interacting with the Amazon Location service.
     * @throws Exception if the AWS region is not found in secure preferences.
     */
    fun getGeoRoutesClient(): GeoRoutesClient? {
        val region = securePreferences.get(REGION) ?: throw Exception("No credentials found")

        if (geoRoutesClient == null) {
            val method = securePreferences.get(METHOD) ?: throw Exception("No credentials found")
            geoRoutesClient = when (method) {
                "apiKey" -> {
                    val apiKey = securePreferences.get(API_KEY) ?: throw Exception("API key not found")
                    generateGeoRoutesClient(region, createEmptyCredentialsProvider(), apiKey)
                }
                else -> {
                    val credentialsProvider = createCredentialsProvider()
                    generateGeoRoutesClient(region, credentialsProvider)
                }
            }
        }
        return geoRoutesClient
    }


    /**
     * Generates a new instance of [LocationClient] with the specified region,
     * credentials provider, and optional API key for request signing.
     *
     * @param region The AWS region to configure for the [LocationClient].
     * @param credentialsProvider The credentials provider for the [LocationClient].
     *                            It supplies the credentials required for authenticating requests.
     * @param apiKey Optional. The API key to use for signing requests. If provided,
     *               an [ApiKeyInterceptor] will be added to the [LocationClient].
     * @return A new instance of [LocationClient] configured with the specified parameters.
     */
    fun generateLocationClient(
        region: String,
        credentialsProvider: CredentialsProvider,
        apiKey: String? = null
    ): LocationClient {
        return LocationClient {
            this.region = region
            this.credentialsProvider = credentialsProvider
            apiKey?.let {
                this.interceptors = mutableListOf(ApiKeyInterceptor(it))
            }
        }
    }


    /**
     * Generates a new instance of [GeoMapsClient] with the specified region,
     * credentials provider, and optional API key for request signing.
     *
     * @param region The AWS region to configure for the [GeoMapsClient].
     * @param credentialsProvider The credentials provider for the [GeoMapsClient].
     *                            It supplies the credentials required for authenticating requests.
     * @param apiKey Optional. The API key to use for signing requests. If provided,
     *               an [ApiKeyInterceptor] will be added to the [GeoMapsClient].
     * @return A new instance of [GeoMapsClient] configured with the specified parameters.
     */

    private fun getSystemProperty(name: String, defaultValue: String = "unknown"): String =
        runCatching { System.getProperty(name) }.getOrDefault(defaultValue)

    fun generateGeoMapsClient(
        region: String,
        credentialsProvider: CredentialsProvider,
        apiKey: String? = null
    ): GeoMapsClient {
        return GeoMapsClient {
            this.region = region
            this.credentialsProvider = credentialsProvider
            apiKey?.let {
                this.interceptors = mutableListOf(ApiKeyInterceptor(it))
            }
        }
    }

    /**
     * Generates a new instance of [GeoPlacesClient] with the specified region,
     * credentials provider, and optional API key for request signing.
     *
     * @param region The AWS region to configure for the [GeoPlacesClient].
     * @param credentialsProvider The credentials provider for the [GeoPlacesClient].
     *                            It supplies the credentials required for authenticating requests.
     * @param apiKey Optional. The API key to use for signing requests. If provided,
     *               an [ApiKeyInterceptor] will be added to the [GeoPlacesClient].
     * @return A new instance of [GeoPlacesClient] configured with the specified parameters.
     */
    fun generateGeoPlacesClient(
        region: String,
        credentialsProvider: CredentialsProvider,
        apiKey: String? = null
    ): GeoPlacesClient {
        return GeoPlacesClient {
            this.region = region
            this.credentialsProvider = credentialsProvider
            apiKey?.let {
                this.interceptors = mutableListOf(ApiKeyInterceptor(it))
            }
        }
    }

    /**
     * Generates a new instance of [GeoRoutesClient] with the specified region,
     * credentials provider, and optional API key for request signing.
     *
     * @param region The AWS region to configure for the [GeoRoutesClient].
     * @param credentialsProvider The credentials provider for the [GeoRoutesClient].
     *                            It supplies the credentials required for authenticating requests.
     * @param apiKey Optional. The API key to use for signing requests. If provided,
     *               an [ApiKeyInterceptor] will be added to the [GeoRoutesClient].
     * @return A new instance of [GeoRoutesClient] configured with the specified parameters.
     */
    fun generateGeoRoutesClient(
        region: String,
        credentialsProvider: CredentialsProvider,
        apiKey: String? = null
    ): GeoRoutesClient {
        return GeoRoutesClient {
            this.region = region
            this.credentialsProvider = credentialsProvider
            apiKey?.let {
                this.interceptors = mutableListOf(ApiKeyInterceptor(it))
            }
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
        if (getCredentialsProvider().accessKeyId == null || getCredentialsProvider().secretKey == null) throw Exception(
            "Failed to get credentials"
        )
        return StaticCredentialsProvider(
            Credentials.invoke(
                accessKeyId = getCredentialsProvider().accessKeyId!!,
                secretAccessKey = getCredentialsProvider().secretKey!!,
                sessionToken = getCredentialsProvider().sessionToken,
                expiration = getCredentialsProvider().expiration
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
        try {
            cognitoIdentityClient ?: run {
                cognitoIdentityClient = generateCognitoIdentityClient(region)
            }
            val identityId = cognitoIdentityClient?.getId(GetIdRequest { this.identityPoolId = identityPoolId })
                ?.identityId ?: throw Exception("Failed to get identity ID")

            if (identityId.isNotEmpty()) {
                val credentials = cognitoIdentityClient?.getCredentialsForIdentity(GetCredentialsForIdentityRequest { this.identityId = identityId })
                    ?.credentials ?: throw Exception("Failed to get credentials")

                requireNotNull(credentials.accessKeyId) { "Access key ID is null" }
                requireNotNull(credentials.secretKey) { "Secret key is null" }
                requireNotNull(credentials.sessionToken) { "Session token is null" }

                cognitoCredentialsProvider = CognitoCredentialsProvider(context, identityId, credentials)
                locationClient = null
                geoMapsClient = null
                geoPlacesClient = null
                geoRoutesClient = null
            }
        } catch (e: HttpException) {
            Log.e("Auth", "Credentials generation failed: ${e.cause} ${e.message}")
            throw HttpException("Credentials generation failed")
        } catch (e: Exception) {
            throw Exception("Credentials generation failed", e)
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
        val method = securePreferences.get(METHOD)
        if (method == "apiKey") return true

        val expirationTimeMillis = customCredentials?.expiration?.epochMilliseconds
            ?: cognitoCredentialsProvider?.getCachedCredentials()?.expiration?.epochMilliseconds
            ?: throw Exception("Failed to get credentials")

        return Instant.now().epochMilliseconds < expirationTimeMillis
    }

    /**
     * Retrieves the Cognito credentials.
     * @return The Credentials instance.
     * @throws Exception If the Cognito provider is not initialized.
     */
    fun getCredentialsProvider(): aws.sdk.kotlin.services.cognitoidentity.model.Credentials {
        return when (securePreferences.get(METHOD)) {
            "apiKey" -> emptyCredentials ?: throw Exception("API key empty credentials not initialized")
            "custom" -> customCredentials ?: throw Exception("Custom credentials not initialized")
            else -> cognitoCredentialsProvider?.getCachedCredentials() ?: throw Exception("Cognito credentials not initialized")
        }
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
     * @throws Exception If the Cognito provider is not initialized.
     */
    suspend fun refresh() {
        val region = securePreferences.get(REGION) ?: throw Exception("No credentials found")
        val method = securePreferences.get(METHOD) ?: throw Exception("No method found")

        when (method) {
            "apiKey" -> return
            "custom" -> {
                customCredentials?.let {
                    credentialsProvider?.let { provider -> setCustomCredentials(provider, region) }
                    return
                }
            }
            else -> {
                cognitoCredentialsProvider?.let {
                    locationClient = null
                    geoMapsClient = null
                    geoPlacesClient = null
                    geoRoutesClient = null
                    it.clearCredentials()
                    verifyAndRefreshCredentials()
                } ?: throw Exception("Refresh is only supported for Cognito credentials. Make sure to use the cognito constructor.")
            }
        }
    }

    /**
     * Clears the Cognito credentials.
     * @throws Exception If the Cognito provider is not initialized.
     */
    fun clear() {
        if (cognitoCredentialsProvider === null) throw Exception("Clear is only supported for Cognito credentials. Make sure to use the cognito constructor.")
        cognitoCredentialsProvider?.clearCredentials()
    }
}