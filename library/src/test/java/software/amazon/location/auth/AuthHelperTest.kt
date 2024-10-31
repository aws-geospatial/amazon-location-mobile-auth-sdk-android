// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import android.content.Context
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import software.amazon.location.auth.utils.AwsRegions
import software.amazon.location.auth.utils.Constants
import software.amazon.location.auth.utils.Constants.IDENTITY_POOL_ID
import software.amazon.location.auth.utils.Constants.TEST_API_KEY
import software.amazon.location.auth.utils.Constants.TEST_IDENTITY_POOL_ID


class AuthHelperTest {

    private lateinit var context: Context
    private lateinit var credentialsProvider: CredentialsProvider
    private lateinit var encryptedSharedPreferences: EncryptedSharedPreferences
    private lateinit var cognitoIdentityClient: CognitoIdentityClient
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        credentialsProvider = mockk(relaxed = true)
        encryptedSharedPreferences = mockk(relaxed = true)
        cognitoIdentityClient = mockk(relaxed = true)
        mockkConstructor(EncryptedSharedPreferences::class)
        mockkConstructor(LocationCredentialsProvider::class)

        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().remove(any()) } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get("region") } returns "us-east-1"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.API_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.ACCESS_KEY_ID) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.SECRET_KEY) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.SESSION_TOKEN) } returns "test"
        every { anyConstructed<EncryptedSharedPreferences>().get(Constants.EXPIRATION) } returns "11111"
        every { anyConstructed<EncryptedSharedPreferences>().get(IDENTITY_POOL_ID) } returns TEST_IDENTITY_POOL_ID
        every { anyConstructed<LocationCredentialsProvider>().generateCognitoIdentityClient("us-east-1") } returns cognitoIdentityClient
        coEvery { anyConstructed<LocationCredentialsProvider>().initializeLocationClient(any()) } just runs
        coEvery { anyConstructed<LocationCredentialsProvider>().initializeLocationClient() } just runs
        coEvery { anyConstructed<LocationCredentialsProvider>().isCredentialsValid() } returns true
        every { anyConstructed<LocationCredentialsProvider>().initPreference(context) } returns encryptedSharedPreferences
        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId creates LocationCredentialsProvider`() {
        runBlocking {
            val provider = AuthHelper.withCognitoIdentityPool(context, TEST_IDENTITY_POOL_ID)
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId and string region creates LocationCredentialsProvider`() {
        runBlocking {
            val provider =
                AuthHelper.withCognitoIdentityPool(context, TEST_IDENTITY_POOL_ID, "us-east-1")
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId and Regions enum creates LocationCredentialsProvider`() {
        runBlocking {
            val provider =
                AuthHelper.withCognitoIdentityPool(
                    context,
                    TEST_IDENTITY_POOL_ID,
                    AwsRegions.US_EAST_1
                )
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithCredentialsProvider with identityPoolId`() {
        runBlocking {
            val provider =
                AuthHelper.withCredentialsProvider(
                    context,
                    credentialsProvider,
                    "us-east-1",
                )
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithApiKey creates LocationCredentialsProvider`() {
        runBlocking {
            val provider = AuthHelper.withApiKey(context, TEST_API_KEY,"us-east-1")
            assertNotNull(provider)
        }
    }
}