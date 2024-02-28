package software.amazon.location.auth

import android.content.Context
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.geo.AmazonLocationClient
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import software.amazon.location.auth.util.AuthCallback

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

    suspend fun authenticateWithUserPoolId(
        identityPoolId: String,
        userPoolId: String,
        clientId: String,
    ): LocationCredentialsProvider? {
        try {
            val mAwsConfiguration = AWSConfiguration(
                JSONObject(
                    getAwsConfigJson(
                        identityPoolId,
                        userPoolId,
                        clientId,
                        identityPoolId.split(":")[0],
                    ),
                ),
            )
            // Initialize the AWSMobileClient and wait for the initialization to complete
            val initializationDeferred = CompletableDeferred<Unit>()
            AWSMobileClient.getInstance().initialize(
                context,
                mAwsConfiguration,
                object : Callback<UserStateDetails> {
                    override fun onResult(result: UserStateDetails?) {
                        initializationDeferred.complete(Unit)
                    }

                    override fun onError(e: Exception?) {
                        initializationDeferred.completeExceptionally(
                            e ?: Exception("Initialization failed"),
                        )
                    }
                },
            )
            initializationDeferred.await()

            return LocationCredentialsProvider(
                context,
                identityPoolId,
                Regions.fromName(identityPoolId.split(":")[0]),
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun userSignIn(
        userName: String,
        password: String,
        callback: AuthCallback,
    ) {
        try {
            // Sign in the user and wait for the sign-in process to complete
            val signInDeferred = CompletableDeferred<Unit>()
            AWSMobileClient.getInstance().signIn(
                userName,
                password,
                null,
                object : Callback<SignInResult> {
                    override fun onResult(signInResult: SignInResult) {
                        when (signInResult.signInState) {
                            SignInState.DONE -> signInDeferred.complete(Unit)
                            SignInState.NEW_PASSWORD_REQUIRED -> {
                                callback.newPasswordRequired()
                                return
                            }

                            else -> signInDeferred.completeExceptionally(Exception("Sign-in state not done"))
                        }
                    }

                    override fun onError(e: Exception) {
                        signInDeferred.completeExceptionally(e)
                    }
                },
            )
            signInDeferred.await()
        } catch (e: Exception) {
            e.printStackTrace()
            callback.signInFailed()
        }
    }

    private fun getAwsConfigJson(
        poolID: String?,
        userPoolId: String? = null,
        appClientId: String?,
        region: String?,
    ): String {
        return "{\n" +
            "    \"UserAgent\": \"aws-amplify-cli/0.1.0\",\n" +
            "    \"Version\": \"0.1.0\",\n" +
            "    \"IdentityManager\": {\n" +
            "        \"Default\": {}\n" +
            "    },\n" +
            "    \"CredentialsProvider\": {\n" +
            "        \"CognitoIdentity\": {\n" +
            "            \"Default\": {\n" +
            "                \"PoolId\": \"$poolID\",\n" +
            "                \"Region\": \"$region\"\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"CognitoUserPool\": {\n" +
            "        \"Default\": {\n" +
            "            \"PoolId\": \"$userPoolId\",\n" +
            "            \"AppClientId\": \"$appClientId\",\n" +
            "            \"Region\": \"$region\"\n" +
            "        }\n" +
            "    }\n" +
            "}"
    }
}
