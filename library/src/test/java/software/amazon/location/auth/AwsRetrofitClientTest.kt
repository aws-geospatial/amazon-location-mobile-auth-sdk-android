package software.amazon.location.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import software.amazon.location.auth.data.network.AwsApiService
import software.amazon.location.auth.data.network.AwsOkHttpClient
import software.amazon.location.auth.data.network.AwsRetrofitClient
import software.amazon.location.auth.utils.Constants.TEST_REGION
import software.amazon.location.auth.utils.Constants.TEST_SERVICE
import software.amazon.location.auth.utils.Constants.TEST_URL1

class AwsRetrofitClientTest {

    private lateinit var mockRetrofit: Retrofit
    private lateinit var mockAwsApiService: AwsApiService
    private lateinit var mockCredentialsProvider: LocationCredentialsProvider
    private lateinit var mockHttpException: HttpException

    @Before
    fun setUp() {
        mockRetrofit = mockk(relaxed = true)
        mockAwsApiService = mockk(relaxed = true)
        mockCredentialsProvider = mockk(relaxed = true)
        mockHttpException = mockk()

        every { mockRetrofit.create(AwsApiService::class.java) } returns mockAwsApiService

        mockkStatic(AwsOkHttpClient::class)
        AwsRetrofitClient.init(TEST_URL1, TEST_SERVICE, TEST_REGION, mockCredentialsProvider)
    }

    @Test
    fun `test init initializes Retrofit and AwsApiService`() {
        assertNotNull(AwsRetrofitClient.apiService)
    }

    @Test
    fun `test apiService returns existing instance if initialized`() {
        val firstInstance = AwsRetrofitClient.apiService
        val secondInstance = AwsRetrofitClient.apiService
        assertNotNull(firstInstance)
        assertNotNull(secondInstance)
        assert(firstInstance === secondInstance) { "Instances should be the same" }
    }

    @Test
    fun `test clearApiService sets _apiService to null`() {
        assertNotNull(AwsRetrofitClient.apiService)
        AwsRetrofitClient.clearApiService()
    }

    @Test
    fun `test isHttpStatusCodeCredentialExpired returns true for expired credential error`() {
        every { mockHttpException.code() } returns RESPONSE_CODE_CREDENTIAL_EXPIRED
        val result = AwsRetrofitClient.isHttpStatusCodeCredentialExpired(mockHttpException)
        assertTrue(result)
    }

    @Test
    fun `test isHttpStatusCodeCredentialExpired returns false for non-HttpException`() {
        val result = AwsRetrofitClient.isHttpStatusCodeCredentialExpired(Exception())
        assertFalse(result)
    }

    companion object {
        const val RESPONSE_CODE_CREDENTIAL_EXPIRED = 403
    }
}
