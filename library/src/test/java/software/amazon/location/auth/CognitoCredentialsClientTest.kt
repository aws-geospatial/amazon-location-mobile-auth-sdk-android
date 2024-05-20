package software.amazon.location.auth

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import software.amazon.location.auth.utils.CognitoCredentialsClient
import software.amazon.location.auth.utils.Constants.JSON
import software.amazon.location.auth.utils.Constants.JSON_2


class CognitoCredentialsClientTest {
    private lateinit var cognitoCredentialsClient: CognitoCredentialsClient

    @Mock
    private lateinit var mockClient: OkHttpClient

    @Mock
    private lateinit var mockCall: Call

    @Mock
    private lateinit var mockResponse: Response

    @Mock
    lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockClient = mockk()
        mockCall = mockk()
        mockResponse = mockk()
        cognitoCredentialsClient = CognitoCredentialsClient("us-east-1")
        cognitoCredentialsClient.client = mockClient
    }

    @Test
    fun `test getIdentityId`() = runBlocking {
        val jsonResponseBody = JSON.toResponseBody("application/json".toMediaTypeOrNull())

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            val callback = arg<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
        }
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns jsonResponseBody


        val result = cognitoCredentialsClient.getIdentityId("us-east-1:xxxxxx-xxxx-xxxx-xxxxx-xxxxxxxxxx")
        assertTrue(result == "us-east-1:xxxxxx-xxxx-xxxx-xxxxx-xxxxxxxxxx")
    }

    @Test
    fun `test getCredentials`() = runBlocking {
        val jsonResponseBody = JSON_2.toResponseBody("application/json".toMediaTypeOrNull())

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.enqueue(any()) } answers {
            val callback = arg<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
        }
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns jsonResponseBody


        val result = cognitoCredentialsClient.getCredentials("us-east-1:xxxxxx-xxxx-xxxx-xxxxx-xxxxxxxxxx")
        assertTrue(result.credentials.secretKey == "test")
    }
}