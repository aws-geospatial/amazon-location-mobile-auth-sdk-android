package software.amazon.location.auth

import android.content.Context
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.geo.AmazonLocationClient

/**
 * Provides methods for authenticating with AWS services using different credentials providers.
 */
class AuthHelper(private val context: Context) {

    /**
     * Authenticates using a Cognito Identity Pool ID and infers the region.
     * @param identityPoolId The identity pool id for authentication.
     * @return A LocationCredentialsProvider object.
     */
    fun authenticateWithCognitoIdentityPool(
        identityPoolId: String,
    ): LocationCredentialsProvider = LocationCredentialsProvider(
        context,
        identityPoolId,
        // Get the region from the identity pool id
        Regions.fromName(identityPoolId.split(":")[0]),
    )

    /**
     * Authenticates using a Cognito Identity Pool ID and a specified region.
     * @param identityPoolId The identity pool id.
     * @param region The AWS region as a string.
     * @return A LocationCredentialsProvider object.
     */
    fun authenticateWithCognitoIdentityPool(
        identityPoolId: String,
        region: String,
    ): LocationCredentialsProvider = LocationCredentialsProvider(
        context,
        identityPoolId,
        Regions.fromName(region),
    )

    /**
     * Authenticates using a Cognito Identity Pool ID and a specified region.
     * @param identityPoolId The identity pool id.
     * @param region The AWS region as a Regions enum.
     * @return A LocationCredentialsProvider object.
     */
    fun authenticateWithCognitoIdentityPool(
        identityPoolId: String,
        region: Regions,
    ): LocationCredentialsProvider = LocationCredentialsProvider(
        context,
        identityPoolId,
        region,
    )

    /**
     * Authenticates using an API key.
     * @param apiKey The API key for authentication.
     * @return A LocationCredentialsProvider instance.
     */
    fun authenticateWithApiKey(
        apiKey: String,
    ): LocationCredentialsProvider = LocationCredentialsProvider(
        context,
        apiKey,
    )

    /**
     * Creates an AmazonLocationClient with the provided credentials provider.
     * @param credentialsProvider The AWS credentials provider.
     * @return An instance of AmazonLocationClient.
     */
    fun getLocationClient(credentialsProvider: AWSCredentialsProvider): AmazonLocationClient =
        AmazonLocationClient(credentialsProvider)
}
