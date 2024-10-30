// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import android.util.Log
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import aws.smithy.kotlin.runtime.http.HttpException
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds

// Provide credentials for the given Cognito identity pool.
class CognitoCredentialsProvider
    /**
     * Create a CognitoCredentialsProvider that handles the given identity pool ID.
     * The credentials themselves will be lazily fetched on the first resolve() call.
     *
     * @param identityPoolId The identity pool ID for Cognito.
     * @param identityRegion The AWS region where the identity pool is located.
     */
    (
        private var identityPoolId: String,// Keep track of the region and identity pool ID for use during credential refreshes.
        private var identityRegion: String
    ) : CredentialsProvider {

    // staticCredentialsProvider holds the cached credentials.
    // (Defaults to empty credentials)
    private var staticCredentialsProvider = StaticCredentialsProvider(
        Credentials.invoke(
            accessKeyId = "",
            secretAccessKey = "",
            sessionToken = null,
            expiration = null
        ))

    /**
     * Provide the credentials, but refresh them first if they've expired.
     *
     * @param attributes
     * @return The Cognito credentials to use for authentication.
     */
    override suspend fun resolve(attributes: Attributes): Credentials {
        if (!areCredentialsValid()) {
            refreshCognitoCredentials()
        }

        return staticCredentialsProvider.credentials
    }

    /**
     * Fetches a new set of credentials from Cognito.
     *
     * All of the Cognito access has been pulled out into this function so that it's easy to mock
     * away the Cognito calls in unit tests by mocking this method. Successfully mocking at the
     * Cognito level is much more difficult due to the nested Builder() calls and lambdas that
     * get invoked.
     *
     * @throws Exception if the credential generation fails.
     */
    suspend fun fetchCognitoCredentials(): aws.sdk.kotlin.services.cognitoidentity.model.Credentials {
        var credentials = aws.sdk.kotlin.services.cognitoidentity.model.Credentials.invoke {}

        try {
            val poolId = identityPoolId
            val cognitoIdentityClient = CognitoIdentityClient { this.region = identityRegion }
            val identityId = cognitoIdentityClient.getId(GetIdRequest { this.identityPoolId = poolId })
                .identityId ?: throw Exception("Failed to get identity ID for identity pool")

            if (identityId.isNotEmpty()) {
                credentials = cognitoIdentityClient.getCredentialsForIdentity(
                    GetCredentialsForIdentityRequest { this.identityId = identityId })
                    .credentials ?: throw Exception("Failed to get credentials for identity")
            }
        } catch (e: HttpException) {
            Log.e("Auth", "Credentials generation failed: ${e.cause} ${e.message}")
            throw HttpException("Credentials generation failed", e)
        } catch (e: Exception) {
            throw Exception("Credentials generation failed", e)
        }

        return credentials
    }

    /**
     * Generates new AWS credentials using the specified region and identity pool ID.
     *
     * This function fetches the identity ID and credentials from Cognito, and then initializes
     * the CognitoCredentialsProvider with the retrieved credentials.
     *
     * @throws Exception if the credential generation fails.
     */
    private suspend fun refreshCognitoCredentials() {
        try {
            val credentials = fetchCognitoCredentials()
            requireNotNull(credentials.accessKeyId) { "Access key ID is null" }
            requireNotNull(credentials.secretKey) { "Secret key is null" }
            requireNotNull(credentials.sessionToken) { "Session token is null" }

            staticCredentialsProvider = StaticCredentialsProvider(
                Credentials.invoke(
                    accessKeyId = credentials.accessKeyId!!,
                    secretAccessKey = credentials.secretKey!!,
                    sessionToken = credentials.sessionToken,
                    expiration = credentials.expiration
                ))
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Check to see if the credentials have expired yet or not.
     */
    private fun areCredentialsValid(): Boolean {
        if (staticCredentialsProvider.credentials.expiration == null) return false

        val expirationTimeMillis = staticCredentialsProvider.credentials.expiration!!.epochMilliseconds
        return Instant.now().epochMilliseconds < expirationTimeMillis
    }
}
