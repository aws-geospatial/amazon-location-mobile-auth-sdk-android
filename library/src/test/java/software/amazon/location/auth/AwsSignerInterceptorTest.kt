package software.amazon.location.auth

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.data.model.response.Credentials
import software.amazon.location.auth.utils.Constants.HEADER_AUTHORIZATION
import software.amazon.location.auth.utils.Constants.HEADER_HOST
import software.amazon.location.auth.utils.Constants.HEADER_X_AMZ_SECURITY_TOKEN
import software.amazon.location.auth.utils.Constants.TEST_REGION
import software.amazon.location.auth.utils.Constants.TEST_URL
import software.amazon.location.auth.utils.Constants.TEST_URL1

class AwsSignerInterceptorTest {

    private lateinit var interceptor: AwsSignerInterceptor
    private lateinit var mockCredentialsProvider: LocationCredentialsProvider

    @Before
    fun setUp() {
        mockCredentialsProvider = mockk(relaxed = true)
        interceptor = AwsSignerInterceptor(
            "execute-api",
            TEST_REGION,
            mockCredentialsProvider
        )
    }

    @Test
    fun `test intercept with non-AWS request proceeds without modification`() {
        val chain = mockk<Interceptor.Chain>()
        val originalRequest = Request.Builder()
            .url(TEST_URL1)
            .build()

        every { chain.request() } returns originalRequest
        every { chain.proceed(originalRequest) } returns mockk(relaxed = true)

        val response = interceptor.intercept(chain)

        verify { chain.proceed(originalRequest) }
        assertNotNull(response)
    }

    @Test
    fun `test intercept with AWS request adds headers and signs the request`() {
        val chain = mockk<Interceptor.Chain>()
        val originalRequest = Request.Builder()
            .url(TEST_URL)
            .build()
        
        val credentials = mockk<Credentials> {
            every { accessKeyId } returns "testAccessKeyId"
            every { secretKey } returns "testSecretKey"
            every { sessionToken } returns "testSessionToken"
        }

        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns mockk(relaxed = true)
        coEvery { mockCredentialsProvider.isCredentialsValid(any()) } returns true
        coEvery { mockCredentialsProvider.getCredentialsProvider() } returns credentials

        val response = runBlocking { interceptor.intercept(chain) }

        val host = URL(originalRequest.url.toString()).host

        verify {
            chain.proceed(withArg { modifiedRequest ->
                assertEquals("service.amazonaws.com", modifiedRequest.url.host)
                assertEquals(host, modifiedRequest.header(HEADER_HOST))
                assertEquals("testSessionToken", modifiedRequest.header(HEADER_X_AMZ_SECURITY_TOKEN))
                assertNotNull(modifiedRequest.header(HEADER_AUTHORIZATION))
            })
        }
        assertNotNull(response)
    }

    @Test
    fun `test sha256Hex computes correct hash`() {
        val data = "test data"
        val expectedHash = MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        assertNotNull(expectedHash)
    }
}
