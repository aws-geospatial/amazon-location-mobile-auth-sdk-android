// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.location.auth.utils.AwsRegions

/**
 * Provides methods for authenticating with AWS services using different credentials providers.
 */
object AuthHelper {

    /**
     * Authenticates using a Cognito Identity Pool ID and infers the region.
     * @param identityPoolId The identity pool id for authentication.
     * @return A LocationCredentialsProvider object.
     */
    suspend fun withCognitoIdentityPool(
        identityPoolId: String,
    ): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                identityPoolId,
                // Get the region from the identity pool id
                AwsRegions.fromName(identityPoolId.split(":")[0]),
            )
            locationCredentialsProvider // Return the generated locationCredentialsProvider
        }
    }

    /**
     * Authenticates using a Cognito Identity Pool ID and a specified region.
     * @param identityPoolId The identity pool id.
     * @param region The AWS region as a string.
     * @return A LocationCredentialsProvider object.
     */
    suspend fun withCognitoIdentityPool(
        identityPoolId: String,
        region: String,
    ): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                identityPoolId,
                AwsRegions.fromName(region),
            )
            locationCredentialsProvider // Return the generated locationCredentialsProvider
        }
    }

    /**
     * Authenticates using a region and a specified CredentialsProvider.
     *
     * For example, to get credentials from AWS Kotlin SDK:
     * 1. Use `CognitoIdentityClient` to call `.getId` to get the identity ID:
     *    ``` kotlin
     *    val identityId = cognitoIdentityClient.getId(GetIdRequest { this.identityPoolId = identityPoolId }).identityId
     *    ```
     *
     * 2. Use `CognitoIdentityClient` to call `.getCredentialsForIdentity` with the identity ID to get the credentials:
     *    ``` kotlin
     *    val credentials = cognitoIdentityClient.getCredentialsForIdentity(GetCredentialsForIdentityRequest { this.identityId = identityId }).credentials
     *    ```
     *
     *
     * To create a `StaticCredentialsProvider` as a `CredentialsProvider` from the obtained credentials:
     * 1. Use the credentials obtained in the previous steps:
     *    ``` kotlin
     *    val staticCredentialsProvider = StaticCredentialsProvider(
     *          aws.smithy.kotlin.runtime.auth.awscredentials.Credentials.invoke(
     *                 accessKeyId = credentials.accessKeyId,
     *                 secretAccessKey = credentials.secretKey,
     *                 sessionToken = credentials.sessionToken,
     *                 expiration = credentials.expiration
     *          )
     *    )
     *    ```
     *
     * @param credentialsProvider The `CredentialsProvider` from AWS Kotlin SDK (`aws.smithy.kotlin.runtime.auth.awscredentials`).
     * @param region The AWS region as a string.
     * @return A `LocationCredentialsProvider` object.
     */
    suspend fun withCredentialsProvider(
        credentialsProvider: CredentialsProvider,
        region: String,
    ): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                credentialsProvider,
                AwsRegions.fromName(region),
            )
            locationCredentialsProvider
        }
    }

    /**
     * Authenticates using an API key.
     * @param apiKey The API key for authentication.
     * @param region The AWS region as a string.
     * @return A LocationCredentialsProvider instance.
     */
    suspend fun withApiKey(apiKey: String, region: String): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                AwsRegions.fromName(region),
                apiKey,
            )
            locationCredentialsProvider
        }
    }
}
