package software.amazon.location.auth

import android.content.Context
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull

private const val TEST_IDENTITY_POOL_ID = "us-east-1:dummyIdentityPoolId"
private const val TEST_API_KEY = "dummyApiKey"

class AuthHelperTest {

    private lateinit var context: Context
    private lateinit var authHelper: AuthHelper

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        authHelper = AuthHelper(context)
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId creates LocationCredentialsProvider`() {
        val provider = authHelper.authenticateWithCognitoIdentityPool(TEST_IDENTITY_POOL_ID)
        assertNotNull(provider)
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId and string region creates LocationCredentialsProvider`() {
        val provider =
            authHelper.authenticateWithCognitoIdentityPool(TEST_IDENTITY_POOL_ID, "us-east-1")
        assertNotNull(provider)
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId and Regions enum creates LocationCredentialsProvider`() {
        val provider =
            authHelper.authenticateWithCognitoIdentityPool(TEST_IDENTITY_POOL_ID, Regions.US_EAST_1)
        assertNotNull(provider)
    }

    @Test
    fun `authenticateWithApiKey creates LocationCredentialsProvider`() {
        val provider = authHelper.authenticateWithApiKey(TEST_API_KEY)
        assertNotNull(provider)
    }

    @Test
    fun `getLocationClient creates AmazonLocationClient with provided credentials provider`() {
        val credentialsProvider = mockk<AWSCredentialsProvider>(relaxed = true)
        val locationClient = authHelper.getLocationClient(credentialsProvider)
        assertNotNull(locationClient)
    }
}