package software.amazon.location.auth

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.amazonaws.regions.Regions
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

private const val TEST_IDENTITY_POOL_ID = "us-east-1:dummyIdentityPoolId"
private const val TEST_API_KEY = "dummyApiKey"

class LocationCredentialsProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkConstructor(AWSKeyValueStore::class)
        every { anyConstructed<AWSKeyValueStore>().put(any(), any<String>()) } just runs
        every { anyConstructed<AWSKeyValueStore>().get("region") } returns "us-east-1"
    }

    @Test
    fun `constructor with Cognito initializes correctly`() {
        every { anyConstructed<AWSKeyValueStore>().get("method") } returns "api"
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, Regions.US_EAST_1)
        assertNotNull(provider)
    }

    @Test
    fun `constructor with API key initializes correctly`() {
        val provider = LocationCredentialsProvider(context, TEST_API_KEY)
        assertNotNull(provider)
    }

    @Test
    fun `constructor with cached credentials for Cognito initializes correctly`() {
        every { anyConstructed<AWSKeyValueStore>().get("method") } returns "cognito"
        every { anyConstructed<AWSKeyValueStore>().get("identityPoolId") } returns TEST_IDENTITY_POOL_ID
        val provider = LocationCredentialsProvider(context)
        assertNotNull(provider)
    }

    @Test
    fun `constructor with cached credentials for API key initializes correctly`() {
        every { anyConstructed<AWSKeyValueStore>().get("method") } returns "apiKey"
        every { anyConstructed<AWSKeyValueStore>().get("apiKey") } returns TEST_API_KEY
        val provider = LocationCredentialsProvider(context)
        assertNotNull(provider)
    }

    @Test
    fun `getCredentialsProvider returns cognito provider successfully`() {
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, Regions.US_EAST_1)
        assertNotNull(provider.getCredentialsProvider())
    }

    @Test
    fun `getApiKeyProvider returns api key provider successfully`() {
        val provider = LocationCredentialsProvider(context, TEST_API_KEY)
        assertNotNull(provider.getApiKeyProvider())
    }

    @Test
    fun `clear successfully clears cognito credentials`() {
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, Regions.US_EAST_1)
        provider.clear()
    }

    @Test
    fun `constructor with cached cognito credentials throws exception on missing data`() {
        every { anyConstructed<AWSKeyValueStore>().get("method") } returns "cognito"
        every { anyConstructed<AWSKeyValueStore>().get("identityPoolId") } returns null // Simulate missing data
        assertFailsWith<Exception> { LocationCredentialsProvider(context) }
    }

    @Test
    fun `constructor with cached API key credentials throws exception on missing data`() {
        every { anyConstructed<AWSKeyValueStore>().get("method") } returns "apiKey"
        every { anyConstructed<AWSKeyValueStore>().get("apiKey") } returns null // Simulate missing data
        assertFailsWith<Exception> { LocationCredentialsProvider(context) }
    }

    @Test
    fun `verify AWSKeyValueStore interactions for cognito initialization`() {
        LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, Regions.US_EAST_1)
        verify(exactly = 1) { anyConstructed<AWSKeyValueStore>().put("method", "cognito") }
        verify(exactly = 1) { anyConstructed<AWSKeyValueStore>().put("identityPoolId",
            TEST_IDENTITY_POOL_ID
        ) }
        verify(exactly = 1) { anyConstructed<AWSKeyValueStore>().put("region", Regions.US_EAST_1.getName()) }
    }

    @Test
    fun `getCredentialsProvider throws if Cognito provider not initialized`() {
        val provider = LocationCredentialsProvider(context, "apiKey")
        assertFailsWith<Exception> { provider.getCredentialsProvider() }
    }

    @Test
    fun `getApiKeyProvider throws if API key provider not initialized`() {
        val provider = LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, Regions.US_EAST_1)
        assertFailsWith<Exception> { provider.getApiKeyProvider() }
    }

    @Test
    fun `refresh throws if Cognito provider not initialized`() {
        val provider = LocationCredentialsProvider(context, "apiKey")
        assertFailsWith<Exception> { provider.refresh() }
    }

    @Test
    fun `clear throws if Cognito provider not initialized`() {
        val provider = LocationCredentialsProvider(context, "apiKey")
        assertFailsWith<Exception> { provider.clear() }
    }
}
