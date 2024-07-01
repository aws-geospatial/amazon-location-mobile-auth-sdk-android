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
     * @param region The AWS region as a string.
     * @param credentialsProvider The CredentialsProvider from AWS kotlin SDK.
     * @return A LocationCredentialsProvider object.
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
}
