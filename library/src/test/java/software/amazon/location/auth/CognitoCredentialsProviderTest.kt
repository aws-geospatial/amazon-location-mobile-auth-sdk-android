package software.amazon.location.auth

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
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
import software.amazon.location.auth.data.response.Credentials
import software.amazon.location.auth.utils.Constants.ACCESS_KEY_ID
import software.amazon.location.auth.utils.Constants.EXPIRATION
import software.amazon.location.auth.utils.Constants.SECRET_KEY
import software.amazon.location.auth.utils.Constants.SESSION_TOKEN

class CognitoCredentialsProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        mockkConstructor(AWSKeyValueStore::class)
        every { anyConstructed<AWSKeyValueStore>().put(any(), any<String>()) } just runs
        every { anyConstructed<AWSKeyValueStore>().get(any()) } returns null
        every { anyConstructed<AWSKeyValueStore>().clear() } just runs
    }

    @Test
    fun `constructor with credentials saves credentials`() {
        val credentials = Credentials("accessKeyId", 1234567890.0, "secretKey", "sessionToken")

        every {
            anyConstructed<AWSKeyValueStore>().put(
                ACCESS_KEY_ID,
                credentials.accessKeyId
            )
        } just runs
        every {
            anyConstructed<AWSKeyValueStore>().put(
                SECRET_KEY,
                credentials.secretKey
            )
        } just runs
        every {
            anyConstructed<AWSKeyValueStore>().put(
                SESSION_TOKEN,
                credentials.sessionToken
            )
        } just runs
        every {
            anyConstructed<AWSKeyValueStore>().put(
                EXPIRATION,
                credentials.expiration.toString()
            )
        } just runs

        CognitoCredentialsProvider(context, credentials)

        verify {
            anyConstructed<AWSKeyValueStore>().put(ACCESS_KEY_ID, credentials.accessKeyId)
            anyConstructed<AWSKeyValueStore>().put(SECRET_KEY, credentials.secretKey)
            anyConstructed<AWSKeyValueStore>().put(SESSION_TOKEN, credentials.sessionToken)
            anyConstructed<AWSKeyValueStore>().put(EXPIRATION, credentials.expiration.toString())
        }
    }

    @Test
    fun `constructor without credentials throws when no credentials found`() {
        every { anyConstructed<AWSKeyValueStore>().get(ACCESS_KEY_ID) } returns null

        assertFailsWith<Exception> {
            CognitoCredentialsProvider(context)
        }
    }

    @Test
    fun `getCachedCredentials returns credentials when found`() {
        val credentials = Credentials("accessKeyId", 1234567890.0, "secretKey", "sessionToken")
        every { anyConstructed<AWSKeyValueStore>().get(ACCESS_KEY_ID) } returns credentials.accessKeyId
        every { anyConstructed<AWSKeyValueStore>().get(SECRET_KEY) } returns credentials.secretKey
        every { anyConstructed<AWSKeyValueStore>().get(SESSION_TOKEN) } returns credentials.sessionToken
        every { anyConstructed<AWSKeyValueStore>().get(EXPIRATION) } returns credentials.expiration.toString()

        val provider = CognitoCredentialsProvider(context, credentials)
        val cachedCredentials = provider.getCachedCredentials()

        assertEquals(credentials, cachedCredentials)
    }

    @Test
    fun `getCachedCredentials returns null when not all credentials are found`() {
        every { anyConstructed<AWSKeyValueStore>().get(ACCESS_KEY_ID) } returns "accessKeyId"
        every { anyConstructed<AWSKeyValueStore>().get(SECRET_KEY) } returns null
        every { anyConstructed<AWSKeyValueStore>().get(SESSION_TOKEN) } returns "sessionToken"
        every { anyConstructed<AWSKeyValueStore>().get(EXPIRATION) } returns "1234567890.0"

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

        every { anyConstructed<AWSKeyValueStore>().get(ACCESS_KEY_ID) } throws Exception("Not initialized")

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

        verify { anyConstructed<AWSKeyValueStore>().clear() }
    }
}
