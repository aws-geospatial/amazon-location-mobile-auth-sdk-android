package software.amazon.location.auth

import android.content.Context
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.utils.AwsRegions
import software.amazon.location.auth.utils.Constants.ACCESS_KEY_ID
import software.amazon.location.auth.utils.Constants.EXPIRATION
import software.amazon.location.auth.utils.Constants.IDENTITY_POOL_ID
import software.amazon.location.auth.utils.Constants.METHOD
import software.amazon.location.auth.utils.Constants.REGION
import software.amazon.location.auth.utils.Constants.SECRET_KEY
import software.amazon.location.auth.utils.Constants.SESSION_TOKEN
import software.amazon.location.auth.utils.Constants.TEST_IDENTITY_POOL_ID

class CustomCredentialsProviderTest {
    private lateinit var context: Context
    private lateinit var locationClient: LocationClient
    private lateinit var cognitoIdentityClient: CognitoIdentityClient
    private lateinit var cognitoCredentialsProvider: CognitoCredentialsProvider
    private lateinit var credentialsProvider: CredentialsProvider

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        cognitoIdentityClient = mockk(relaxed = true)
        cognitoCredentialsProvider = mockk(relaxed = true)
        credentialsProvider = mockk(relaxed = true)
        mockkConstructor(EncryptedSharedPreferences::class)
        mockkConstructor(CognitoCredentialsProvider::class)
        mockkConstructor(LocationCredentialsProvider::class)
        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs

        every { anyConstructed<LocationCredentialsProvider>().generateCognitoIdentityClient("us-east-1") } returns cognitoIdentityClient
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(REGION) } returns "us-east-1"
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().remove(any()) } just runs
    }

    @Test
    fun `getCredentialsProvider returns cognito provider successfully with custom credential`() {
        val expirationTime =
            Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds + 10000) // 10 seconds in the future
        val mockCredentials =
            Credentials.invoke {
                expiration = expirationTime
                secretKey = "test"
                accessKeyId = "test"
                sessionToken = "test"
            }
        every { anyConstructed<CognitoCredentialsProvider>().getCachedCredentials() } returns mockCredentials
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns ""
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        val provider =
            LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        runBlocking {
            provider.verifyAndRefreshCredentials()
            assertNotNull(provider.getCredentialsProvider())
        }
    }
}
