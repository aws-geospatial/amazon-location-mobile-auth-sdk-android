package software.amazon.location.auth

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import software.amazon.location.auth.utils.AwsRegions
import software.amazon.location.auth.utils.Constants.ACCESS_KEY_ID
import software.amazon.location.auth.utils.Constants.EXPIRATION
import software.amazon.location.auth.utils.Constants.SECRET_KEY
import software.amazon.location.auth.utils.Constants.SESSION_TOKEN

private const val TEST_IDENTITY_POOL_ID = "us-east-1:dummyIdentityPoolId"
private const val TEST_API_KEY = "dummyApiKey"

class LocationCredentialsProviderTest {

    private lateinit var context: Context
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkConstructor(EncryptedSharedPreferences::class)

        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get("region") } returns "us-east-1"
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs
    }

    @Test
    fun `constructor with Cognito initializes correctly`() {
        every { anyConstructed<EncryptedSharedPreferences>().get("method") } returns "api"
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        assertNotNull(provider)
    }

    @Test
    fun `constructor with API key initializes correctly`() {
        val provider = LocationCredentialsProvider(context, TEST_API_KEY)
        assertNotNull(provider)
    }

    @Test
    fun `constructor with cached credentials for Cognito initializes correctly`() {
        every { anyConstructed<EncryptedSharedPreferences>().get("method") } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        every { anyConstructed<EncryptedSharedPreferences>().get("identityPoolId") } returns TEST_IDENTITY_POOL_ID
        val provider = LocationCredentialsProvider(context)
        assertNotNull(provider)
    }

    @Test
    fun `constructor with cached credentials for API key initializes correctly`() {
        every { anyConstructed<EncryptedSharedPreferences>().get("method") } returns "apiKey"
        every { anyConstructed<EncryptedSharedPreferences>().get("apiKey") } returns TEST_API_KEY
        val provider = LocationCredentialsProvider(context)
        assertNotNull(provider)
    }

    @Test
    fun `getCredentialsProvider returns cognito provider successfully`(){
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        coroutineScope.launch {
            provider.generateCredentials()
            assertNotNull(provider.getCredentialsProvider())
        }
    }

    @Test
    fun `getApiKeyProvider returns api key provider successfully`() {
        val provider = LocationCredentialsProvider(context, TEST_API_KEY)
        assertNotNull(provider.getApiKeyProvider())
    }

    @Test
    fun `clear successfully clears cognito credentials`() {
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        coroutineScope.launch {
            provider.generateCredentials()
            provider.clear()
        }
    }

    @Test
    fun `constructor with cached cognito credentials throws exception on missing data`() {
        every { anyConstructed<EncryptedSharedPreferences>().get("method") } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get("identityPoolId") } returns null // Simulate missing data
        assertFailsWith<Exception> { LocationCredentialsProvider(context) }
    }

    @Test
    fun `constructor with cached API key credentials throws exception on missing data`() {
        every { anyConstructed<EncryptedSharedPreferences>().get("method") } returns "apiKey"
        every { anyConstructed<EncryptedSharedPreferences>().get("apiKey") } returns null // Simulate missing data
        assertFailsWith<Exception> { LocationCredentialsProvider(context) }
    }

    @Test
    fun `verify SecurePreferences interactions for cognito initialization`() {
        LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        verify(exactly = 1) { anyConstructed<EncryptedSharedPreferences>().put("method", "cognito") }
        verify(exactly = 1) { anyConstructed<EncryptedSharedPreferences>().put("identityPoolId",
            TEST_IDENTITY_POOL_ID
        ) }
        verify(exactly = 1) { anyConstructed<EncryptedSharedPreferences>().put("region", AwsRegions.US_EAST_1.regionName) }
    }

    @Test
    fun `getCredentialsProvider throws if Cognito provider not initialized`() {
        val provider = LocationCredentialsProvider(context, "apiKey")
        assertFailsWith<Exception> { provider.getCredentialsProvider() }
    }

    @Test
    fun `getApiKeyProvider throws if API key provider not initialized`() {
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        assertFailsWith<Exception> { provider.getApiKeyProvider() }
    }

    @Test
    fun `refresh throws if Cognito provider not initialized`() {
        val provider = LocationCredentialsProvider(context, "apiKey")
        coroutineScope.launch {
            provider.generateCredentials()
            assertFailsWith<Exception> { provider.refresh() }
        }
    }

    @Test
    fun `clear throws if Cognito provider not initialized`() {
        val provider = LocationCredentialsProvider(context, "apiKey")
        assertFailsWith<Exception> { provider.clear() }
    }
}
