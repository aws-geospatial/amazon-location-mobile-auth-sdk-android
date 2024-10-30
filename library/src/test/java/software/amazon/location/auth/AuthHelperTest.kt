// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import software.amazon.location.auth.utils.Constants.TEST_API_KEY
import software.amazon.location.auth.utils.Constants.TEST_IDENTITY_POOL_ID


class AuthHelperTest {

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId creates LocationCredentialsProvider`() {
        runBlocking {
            val provider = AuthHelper.withCognitoIdentityPool(TEST_IDENTITY_POOL_ID)
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithCognitoIdentityPool with identityPoolId and string region creates LocationCredentialsProvider`() {
        runBlocking {
            val provider =
                AuthHelper.withCognitoIdentityPool(TEST_IDENTITY_POOL_ID, "us-east-1")
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithCredentialsProvider with identityPoolId`() {
        val credentialsProvider = mockk<StaticCredentialsProvider>()

        runBlocking {
            val provider =
                AuthHelper.withCredentialsProvider(
                    credentialsProvider,
                    "us-east-1"
                )
            assertNotNull(provider)
        }
    }

    @Test
    fun `authenticateWithApiKey creates LocationCredentialsProvider`() {
        runBlocking {
            val provider = AuthHelper.withApiKey(TEST_API_KEY,"us-east-1")
            assertNotNull(provider)
        }
    }
}