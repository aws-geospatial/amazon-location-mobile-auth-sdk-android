package software.amazon.location.auth

import android.content.Context
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityResponse
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdResponse
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
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

class LocationCredentialsProviderTest {
    private lateinit var context: Context
    private lateinit var locationClient: LocationClient
    private lateinit var cognitoIdentityClient: CognitoIdentityClient
    private lateinit var cognitoCredentialsProvider: CognitoCredentialsProvider
    private lateinit var credentialsProvider: CredentialsProvider

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        locationClient = mockk(relaxed = true)
        cognitoIdentityClient = mockk(relaxed = true)
        cognitoCredentialsProvider = mockk(relaxed = true)
        credentialsProvider = mockk(relaxed = true)
        mockkConstructor(EncryptedSharedPreferences::class)
        mockkConstructor(CognitoCredentialsProvider::class)
        mockkConstructor(LocationCredentialsProvider::class)
        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs

        every { anyConstructed<LocationCredentialsProvider>().generateCognitoIdentityClient("us-east-1") } returns cognitoIdentityClient
        every {
            anyConstructed<LocationCredentialsProvider>().generateLocationClient(
                "us-east-1",
                any(),
            )
        } returns locationClient
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(REGION) } returns "us-east-1"
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().remove(any()) } just runs
    }

    @Test
    fun `constructor with cached credentials for Cognito initializes correctly`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        val provider = LocationCredentialsProvider(context)
        assertNotNull(provider)
    }

    @Test
    fun `getCredentialsProvider returns cognito provider successfully`() {
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

    @Test
    fun `isCredentialsValid returns true when credentials are valid`() {
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
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        val provider =
            LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        runBlocking {
            provider.verifyAndRefreshCredentials()
            provider.refresh()
            val result = provider.isCredentialsValid()
            assertTrue(result)
        }
    }

    @Test
    fun `isCredentialsValid returns false when credentials are expired`() {
        val expirationTime =
            Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds - 10000) // 10 seconds in the past
        val mockCredentials =
            mockk<Credentials> {
                every { expiration } returns expirationTime
            }
        every { anyConstructed<CognitoCredentialsProvider>().getCachedCredentials() } returns mockCredentials
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        val provider =
            LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        runBlocking {
            provider.verifyAndRefreshCredentials()
            provider.refresh()
            val result = provider.isCredentialsValid()
            assertFalse(result)
        }
    }

    @Test
    fun `clear successfully clears cognito credentials`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        val provider =
            LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        runBlocking {
            provider.verifyAndRefreshCredentials()
            provider.clear()
        }
    }

    @Test
    fun `check credentials`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        val provider =
            LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        runBlocking {
            provider.verifyAndRefreshCredentials()
        }
    }

    @Test
    fun `get Location Client`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(EXPIRATION) } returns "11111"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        val identityId = "test-identity-id"
        val credentials =
            Credentials {
                accessKeyId = "test-access-key"
                secretKey = "test-secret-key"
                sessionToken = "test-session-token"
            }

        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        val provider =
            LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        coEvery { cognitoIdentityClient.getId(any<GetIdRequest>()) } returns
            GetIdResponse {
                this.identityId = identityId
            }

        coEvery { cognitoIdentityClient.getCredentialsForIdentity(any<GetCredentialsForIdentityRequest>()) } returns
            GetCredentialsForIdentityResponse {
                this.credentials = credentials
            }
        runBlocking {
            provider.verifyAndRefreshCredentials()
            val locationClient = provider.getLocationClient()
            assertNotNull(locationClient)
        }
    }

    @Test
    fun `constructor with cached cognito credentials throws exception on missing data`() {
        every { anyConstructed<EncryptedSharedPreferences>().get(METHOD) } returns "cognito"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns null // Simulate missing data
        assertFailsWith<Exception> { LocationCredentialsProvider(context) }
    }

    @Test
    fun `verify SecurePreferences interactions for cognito initialization`() {
        LocationCredentialsProvider(context, TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        verify(exactly = 1) { anyConstructed<EncryptedSharedPreferences>().put(METHOD, "cognito") }
        verify(exactly = 1) {
            anyConstructed<EncryptedSharedPreferences>().put(
                IDENTITY_POOL_ID,
                TEST_IDENTITY_POOL_ID,
            )
        }
        verify(exactly = 1) {
            anyConstructed<EncryptedSharedPreferences>().put(
                REGION,
                AwsRegions.US_EAST_1.regionName,
            )
        }
    }
}
