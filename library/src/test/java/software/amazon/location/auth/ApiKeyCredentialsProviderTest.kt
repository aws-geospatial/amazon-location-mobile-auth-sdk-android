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
import software.amazon.location.auth.utils.Constants.API_KEY_TEST
import software.amazon.location.auth.utils.Constants.REGION

private const val TEST_API_KEY = "dummyApiKey"

class ApiKeyCredentialsProviderTest {

    private lateinit var context: Context
    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        mockkConstructor(EncryptedSharedPreferences::class)

        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(REGION) } returns "us-east-1"
        every { anyConstructed<EncryptedSharedPreferences>().remove(any()) } just runs
    }

    @Test
    fun `constructor with apiKey saves credentials`() {
        every { anyConstructed<EncryptedSharedPreferences>().put(API_KEY_TEST, TEST_API_KEY) } just runs

        ApiKeyCredentialsProvider(context, TEST_API_KEY)

        verify { anyConstructed<EncryptedSharedPreferences>().put(API_KEY_TEST, TEST_API_KEY) }
    }

    @Test
    fun `constructor without apiKey throws when no credentials found`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(API_KEY_TEST) } returns null

        assertFailsWith<Exception> {
            ApiKeyCredentialsProvider(context)
        }
    }

    @Test
    fun `getCachedCredentials returns apiKey when found`() {
        val apiKey = "testApiKey"
        every { anyConstructed<EncryptedSharedPreferences>().get(API_KEY_TEST) } returns apiKey

        val provider = ApiKeyCredentialsProvider(context, apiKey)
        val cachedApiKey = provider.getCachedCredentials()

        assertEquals(apiKey, cachedApiKey)
    }

    @Test
    fun `getCachedCredentials throws when not initialized`() {
        val provider = ApiKeyCredentialsProvider(context, "testApiKey")

        every { anyConstructed<EncryptedSharedPreferences>().get(API_KEY_TEST) } throws Exception("Not initialized")

        assertFailsWith<Exception> {
            provider.getCachedCredentials()
        }
    }

    @Test
    fun `clearCredentials clears the stored credentials`() {
        val provider = ApiKeyCredentialsProvider(context, "testApiKey")
        provider.clearCredentials()

        verify { anyConstructed<EncryptedSharedPreferences>().remove(any()) }
    }
}