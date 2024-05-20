package software.amazon.location.auth

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import software.amazon.location.auth.utils.AwsRegions

private const val TEST_IDENTITY_POOL_ID = "us-east-1:dummyIdentityPoolId"
private const val TEST_API_KEY = "dummyApiKey"

class AuthHelperTest {

    private lateinit var context: Context
    private lateinit var authHelper: AuthHelper
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        authHelper = AuthHelper(context)
        mockkConstructor(EncryptedSharedPreferences::class)

        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get("region") } returns "us-east-1"
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId creates LocationCredentialsProvider`() {
        coroutineScope.launch {
            val provider = authHelper.authenticateWithCognitoIdentityPool(TEST_IDENTITY_POOL_ID)
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId and string region creates LocationCredentialsProvider`() {
        coroutineScope.launch {
            val provider =
                authHelper.authenticateWithCognitoIdentityPool(TEST_IDENTITY_POOL_ID, "us-east-1")
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId and Regions enum creates LocationCredentialsProvider`() {
        coroutineScope.launch {
            val provider =
                authHelper.authenticateWithCognitoIdentityPool(
                    TEST_IDENTITY_POOL_ID,
                    AwsRegions.US_EAST_1
                )
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithApiKey creates LocationCredentialsProvider`() {
        val provider = authHelper.authenticateWithApiKey(TEST_API_KEY)
        assertNotNull(provider)
    }
}