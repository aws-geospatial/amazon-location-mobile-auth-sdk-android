package software.amazon.location.auth

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test

private const val TEST_API_KEY = "dummyApiKey"

class ApiKeyCredentialsProviderTest {

    private lateinit var context: Context
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
    fun `constructor with apiKey saves credentials`() {
        every { anyConstructed<EncryptedSharedPreferences>().put("apiKey", TEST_API_KEY) } just runs

        ApiKeyCredentialsProvider(context, TEST_API_KEY)

        verify { anyConstructed<EncryptedSharedPreferences>().put("apiKey", TEST_API_KEY) }
    }

    @Test
    fun `constructor without apiKey throws when no credentials found`() {
        every { anyConstructed<EncryptedSharedPreferences>().get("apiKey") } returns null

        assertFailsWith<Exception> {
            ApiKeyCredentialsProvider(context)
        }
    }

    @Test
    fun `getCachedCredentials returns apiKey when found`() {
        val apiKey = "testApiKey"
        every { anyConstructed<EncryptedSharedPreferences>().get("apiKey") } returns apiKey

        val provider = ApiKeyCredentialsProvider(context, apiKey)
        val cachedApiKey = provider.getCachedCredentials()

        assertEquals(apiKey, cachedApiKey)
    }

    @Test
    fun `getCachedCredentials throws when not initialized`() {
        val provider = ApiKeyCredentialsProvider(context, "testApiKey")

        every { anyConstructed<EncryptedSharedPreferences>().get("apiKey") } throws Exception("Not initialized")

        assertFailsWith<Exception> {
            provider.getCachedCredentials()
        }
    }

    @Test
    fun `clearCredentials clears the stored credentials`() {
        val provider = ApiKeyCredentialsProvider(context, "testApiKey")

        provider.clearCredentials()

        verify { anyConstructed<EncryptedSharedPreferences>().clear() }
    }
}