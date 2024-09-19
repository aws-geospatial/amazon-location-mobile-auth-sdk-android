package software.amazon.location.auth

import android.content.Context
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.location.auth.utils.AwsRegions

/**
 * Provides methods for authenticating with AWS services using different credentials providers.
 */
class AuthHelper(private val context: Context) {

    /**
     * Authenticates using a Cognito Identity Pool ID and infers the region.
     * @param identityPoolId The identity pool id for authentication.
     * @return A LocationCredentialsProvider object.
     */
    suspend fun authenticateWithCognitoIdentityPool(
        identityPoolId: String,
    ): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                context,
                identityPoolId,
                // Get the region from the identity pool id
                AwsRegions.fromName(identityPoolId.split(":")[0]),
            )
            locationCredentialsProvider.verifyAndRefreshCredentials()
            locationCredentialsProvider // Return the generated locationCredentialsProvider
        }
    }

    /**
     * Authenticates using a Cognito Identity Pool ID and a specified region.
     * @param identityPoolId The identity pool id.
     * @param region The AWS region as a string.
     * @return A LocationCredentialsProvider object.
     */
    suspend fun authenticateWithCognitoIdentityPool(
        identityPoolId: String,
        region: String,
    ): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                context,
                identityPoolId,
                AwsRegions.fromName(region),
            )
            locationCredentialsProvider.verifyAndRefreshCredentials()
            locationCredentialsProvider // Return the generated locationCredentialsProvider
        }
    }

    /**
     * Authenticates using a Cognito Identity Pool ID and a specified region.
     * @param identityPoolId The identity pool id.
     * @param region The AWS region as a Regions enum.
     * @return A LocationCredentialsProvider object.
     */
    suspend fun authenticateWithCognitoIdentityPool(
        identityPoolId: String,
        region: AwsRegions,
    ): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                context,
                identityPoolId,
                region,
            )
            locationCredentialsProvider.verifyAndRefreshCredentials()
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
     * @param region The AWS region as a string.
     * @param credentialsProvider The `CredentialsProvider` from AWS Kotlin SDK (`aws.smithy.kotlin.runtime.auth.awscredentials`).
     * @return A `LocationCredentialsProvider` object.
     */
    suspend fun authenticateWithCredentialsProvider(
        region: String,
        credentialsProvider: CredentialsProvider
    ): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                context,
                AwsRegions.fromName(region),
            )
            locationCredentialsProvider.initializeLocationClient(credentialsProvider)
            locationCredentialsProvider
        }
    }

    /**
     * Authenticates using an API key.
     * @param apiKey The API key for authentication.
     * @param region The AWS region as a string.
     * @return A LocationCredentialsProvider instance.
     */
    suspend fun authenticateWithApiKey(apiKey: String, region: String): LocationCredentialsProvider {
        return withContext(Dispatchers.IO) {
            val locationCredentialsProvider = LocationCredentialsProvider(
                context,
                AwsRegions.fromName(region),
                apiKey,
            )
            locationCredentialsProvider.initializeLocationClient()
            locationCredentialsProvider
        }
    }
}
