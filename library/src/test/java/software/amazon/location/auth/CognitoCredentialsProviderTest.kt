package software.amazon.location.auth

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.data.model.response.Credentials
import software.amazon.location.auth.utils.Constants.ACCESS_KEY_ID
import software.amazon.location.auth.utils.Constants.EXPIRATION
import software.amazon.location.auth.utils.Constants.SECRET_KEY
import software.amazon.location.auth.utils.Constants.SESSION_TOKEN

class CognitoCredentialsProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        mockkConstructor(EncryptedSharedPreferences::class)
        mockkConstructor(EncryptedSharedPreferences::class)

        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(any()) } returns null
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().remove(any()) } just runs
    }

    @Test
    fun `constructor with credentials saves credentials`() {
        val credentials = Credentials("accessKeyId", 1234567890.0, "secretKey", "sessionToken")

        every {
            anyConstructed<EncryptedSharedPreferences>().put(
                ACCESS_KEY_ID,
                credentials.accessKeyId
            )
        } just runs
        every {
            anyConstructed<EncryptedSharedPreferences>().put(
                SECRET_KEY,
                credentials.secretKey
            )
        } just runs
        every {
            anyConstructed<EncryptedSharedPreferences>().put(
                SESSION_TOKEN,
                credentials.sessionToken
            )
        } just runs
        every {
            anyConstructed<EncryptedSharedPreferences>().put(
                EXPIRATION,
                credentials.expiration.toString()
            )
        } just runs

        CognitoCredentialsProvider(context, credentials)

        verify {
            anyConstructed<EncryptedSharedPreferences>().put(ACCESS_KEY_ID, credentials.accessKeyId)
            anyConstructed<EncryptedSharedPreferences>().put(SECRET_KEY, credentials.secretKey)
            anyConstructed<EncryptedSharedPreferences>().put(SESSION_TOKEN, credentials.sessionToken)
            anyConstructed<EncryptedSharedPreferences>().put(EXPIRATION, credentials.expiration.toString())
        }
    }

    @Test
    fun `constructor without credentials throws when no credentials found`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns null

        assertFailsWith<Exception> {
            CognitoCredentialsProvider(context)
        }
    }

    @Test
    fun `getCachedCredentials returns credentials when found`() {
        val credentials = Credentials("accessKeyId", 1234567890.0, "secretKey", "sessionToken")
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns credentials.accessKeyId
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns credentials.secretKey
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns credentials.sessionToken
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns credentials.expiration.toString()

        val provider = CognitoCredentialsProvider(context, credentials)
        val cachedCredentials = provider.getCachedCredentials()

        assertEquals(credentials, cachedCredentials)
    }

    @Test
    fun `getCachedCredentials returns null when not all credentials are found`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "accessKeyId"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns null
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "sessionToken"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "1234567890.0"

        val provider = try {
            CognitoCredentialsProvider(context)
        } catch (e: Exception) {
            null
        }

        val cachedCredentials = provider?.getCachedCredentials()

        assertNull(cachedCredentials)
    }

    @Test
    fun `getCachedCredentials throws when not initialized`() {
        val provider = CognitoCredentialsProvider(
            context,
            Credentials("accessKeyId", 1234567890.0, "secretKey", "sessionToken")
        )

        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } throws Exception("Not initialized")

        assertFailsWith<Exception> {
            provider.getCachedCredentials()
        }
    }

    @Test
    fun `clearCredentials clears the stored credentials`() {
        val provider = CognitoCredentialsProvider(
            context,
            Credentials("accessKeyId", 1234567890.0, "secretKey", "sessionToken")
        )

        provider.clearCredentials()

        verify { anyConstructed<EncryptedSharedPreferences>().remove(any()) }
    }
}
