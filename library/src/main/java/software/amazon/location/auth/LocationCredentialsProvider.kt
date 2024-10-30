// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.sdk.kotlin.services.geomaps.GeoMapsClient
import aws.sdk.kotlin.services.geoplaces.GeoPlacesClient
import aws.sdk.kotlin.services.georoutes.GeoRoutesClient
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import software.amazon.location.auth.utils.AwsRegions

/**
 * Provides credentials for accessing location-based services through Cognito authentication,
 * custom authentication, or API keys.
 **/
class LocationCredentialsProvider {
    private var credentialsProvider : CredentialsProvider

    private var identityPoolId: String? = null
    private var method: String
    private var region: AwsRegions? = null
    private var apiKey: String? = null

    /**
     * Initializes with Cognito credentials.
     * @param identityPoolId The identity pool ID for Cognito authentication.
     * @param region The region for Cognito authentication.
     */
    constructor(identityPoolId: String, region: AwsRegions) {
        this.method = "cognito"
        this.identityPoolId = identityPoolId
        this.region = region
        this.credentialsProvider = CognitoCredentialsProvider(identityPoolId, region.regionName)
    }

    /**
     * Initializes with custom authentication.
     * @param credentialsProvider The custom credentials provider to use for authentication.
     * @param region The region for authentication.
     */
    constructor(credentialsProvider: CredentialsProvider, region: AwsRegions) {
        this.method = "custom"
        this.region = region
        this.credentialsProvider = credentialsProvider
    }

    /**
     * Initializes with an API key.
     * NOTE: The order of the parameters are important here to distinguish this constructor
     * from the Cognito constructor that takes in (identityPoolId, region).
     * @param region The region for Cognito authentication.
     * @param apiKey The API key for authentication.
     */
    constructor(region: AwsRegions, apiKey: String) {
        this.method = "apiKey"
        this.apiKey = apiKey
        this.region = region
        // API Keys "empty out" the credentials provider, since the authentication happens
        // via an HTTP interceptor that adds the API key to the HTTP request.
        this.credentialsProvider = StaticCredentialsProvider(
            Credentials.invoke(
                accessKeyId = "",
                secretAccessKey = "",
                sessionToken = null,
                expiration = null
            ))
    }

    /**
     * Get the config for constructing a LocationClient() with the correct credentials.
     * Note that this actually returns a lambda that builds the config, since the LocationClient
     * constructor actually takes in a builder lambda, not a config.
     * Usage looks like this:
     *     val client = LocationClient(provider.getLocationClientConfig())
     * @throws Exception If credentials are not found.
     * @return Lambda that builds a LocationClient config.
     */
    fun getLocationClientConfig() : LocationClient.Config.Builder.() -> Unit  {
        val region = this.region?.regionName ?: throw Exception("No valid region provided")
        val apiKey = this.apiKey
        val credentialsProvider = this.credentialsProvider
        return {
            this.region = region
            this.credentialsProvider = credentialsProvider
            if (apiKey != null) {
                this.interceptors = mutableListOf(ApiKeyInterceptor(apiKey))
            }
        }
    }

    /**
     * Get the config for constructing a GeoMapsClient() with the correct credentials.
     * Note that this actually returns a lambda that builds the config, since the GeoMapsClient
     * constructor actually takes in a builder lambda, not a config.
     * Usage looks like this:
     *     val client = GeoMapsClient(provider.getGeoMapsClientConfig())
     * @throws Exception If credentials are not found.
     * @return Lambda that builds a GeoMapsClient config.
     */
    fun getGeoMapsClientConfig() : GeoMapsClient.Config.Builder.() -> Unit  {
        val region = this.region?.regionName ?: throw Exception("No valid region provided")
        val apiKey = this.apiKey
        val credentialsProvider = this.credentialsProvider
        return {
            this.region = region
            this.credentialsProvider = credentialsProvider
            if (apiKey != null) {
                this.interceptors = mutableListOf(ApiKeyInterceptor(apiKey))
            }
        }
    }

    /**
     * Get the config for constructing a GeoPlacesClient() with the correct credentials.
     * Note that this actually returns a lambda that builds the config, since the GeoPlacesClient
     * constructor actually takes in a builder lambda, not a config.
     * Usage looks like this:
     *     val client = GeoPlacesClient(provider.getGeoPlacesClientConfig())
     * @throws Exception If credentials are not found.
     * @return Lambda that builds a GeoPlacesClient config.
     */
    fun getGeoPlacesClientConfig() : GeoPlacesClient.Config.Builder.() -> Unit  {
        val region = this.region?.regionName ?: throw Exception("No valid region provided")
        val apiKey = this.apiKey
        val credentialsProvider = this.credentialsProvider
        return {
            this.region = region
            this.credentialsProvider = credentialsProvider
            if (apiKey != null) {
                this.interceptors = mutableListOf(ApiKeyInterceptor(apiKey))
            }
        }
    }

    /**
     * Get the config for constructing a GeoRoutesClient() with the correct credentials.
     * Note that this actually returns a lambda that builds the config, since the GeoRoutesClient
     * constructor actually takes in a builder lambda, not a config.
     * Usage looks like this:
     *     val client = GeoRoutesClient(provider.getGeoRoutesClientConfig())
     * @throws Exception If credentials are not found.
     * @return Lambda that builds a GeoRoutesClient config.
     */
    fun getGeoRoutesClientConfig() : GeoRoutesClient.Config.Builder.() -> Unit  {
        val region = this.region?.regionName ?: throw Exception("No valid region provided")
        val apiKey = this.apiKey
        val credentialsProvider = this.credentialsProvider
        return {
            this.region = region
            this.credentialsProvider = credentialsProvider
            if (apiKey != null) {
                this.interceptors = mutableListOf(ApiKeyInterceptor(apiKey))
            }
        }
    }

    /**
     * Retrieves the method type ("cognito", "custom", "apiKey")
     * This should only be needed by the AwsSignerInterceptor.
     * @return The method type.
     */
    fun getMethod() : String {
        return method
    }

    /**
     * Retrieves the API Key if one was provided.
     * This should only be needed by the AwsSignerInterceptor.
     * @return The API Key.
     */
    fun getApiKey(): String? {
        return apiKey
    }

    /**
     * Returns the CredentialsProvider.
     * This can either be a CognitoCredentialsProvider, a custom CredentialsProvider, or an empty StaticCredentialsProvider.
     * @return The CredentialsProvider.
     */
    fun getCredentialsProvider(): CredentialsProvider {
        return credentialsProvider
    }

    /**
     * Retrieves the AWS credentials.
     * @return The Credentials instance containing the accessKeyId, secretAccessKey, and sessionToken
     * @throws Exception If the credentials provider is not initialized.
     */
    suspend fun getCredentials(): Credentials {
        return credentialsProvider.resolve()
    }
}
