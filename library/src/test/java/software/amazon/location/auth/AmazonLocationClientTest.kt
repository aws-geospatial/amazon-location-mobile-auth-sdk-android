
package software.amazon.location.auth

import android.content.Context
import aws.sdk.kotlin.services.location.LocationClient
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.utils.Constants

class AmazonLocationClientTest {

    private lateinit var context: Context
    private lateinit var mockLocationClient: LocationClient
    private lateinit var amazonLocationClient: AmazonLocationClient
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockLocationClient = mockk(relaxed = true)
        mockkConstructor(EncryptedSharedPreferences::class)

        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.REGION) } returns "us-east-1"
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs

    }

    @Test
    fun `test reverseGeocode with valid response`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.EXPIRATION) } returns "11111"
        amazonLocationClient = AmazonLocationClient(mockLocationClient)
        coroutineScope.launch {
            amazonLocationClient.reverseGeocode("indexName", 0.0, 0.0, "en", 10)
            assertNotNull(amazonLocationClient)
        }
    }

}
