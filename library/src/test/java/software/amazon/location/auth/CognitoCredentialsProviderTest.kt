// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import io.mockk.coEvery
import io.mockk.mockkConstructor
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CognitoCredentialsProviderTest {
    @Test
    fun `Constructs successfully with identity pool and region`() {
        val provider = CognitoCredentialsProvider("identityPool", "us-east-1")
        assertNotNull(provider)
    }

    @Test
    fun `Calling resolve for the first time refreshes credentials successfully and returns them`() {
        val expirationTime =
            Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds + 10000)

        // Create mock CognitoIdentityClient
        mockkConstructor(CognitoCredentialsProvider::class)
        coEvery {
            anyConstructed<CognitoCredentialsProvider>().fetchCognitoCredentials()
        } returns Credentials.invoke {
                expiration = expirationTime
                secretKey = "testSecretKey"
                accessKeyId = "testAccessKeyId"
                sessionToken = "testSessionToken"
        }

        val provider = CognitoCredentialsProvider("identityPool", "us-east-1")

        runBlocking {

            // Call resolve
            val credentials = provider.resolve()

            // Verify the returned credentials match what we expected
            assertEquals("testAccessKeyId", credentials.accessKeyId)
            assertEquals("testSessionToken", credentials.sessionToken)
            assertEquals(expirationTime, credentials.expiration)
        }
    }
    @Test
    fun `If credentials aren't expired they are returned successfully from the cache`() {
        val expirationTime =
            Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds + 10000)
        val expirationTime2 =
            Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds + 20000)

        // Create mock CognitoIdentityClient
        mockkConstructor(CognitoCredentialsProvider::class)
        coEvery {
            anyConstructed<CognitoCredentialsProvider>().fetchCognitoCredentials()
        } returns Credentials.invoke {
            expiration = expirationTime
            secretKey = "testSecretKey"
            accessKeyId = "testAccessKeyId"
            sessionToken = "testSessionToken"
        } andThen Credentials.invoke {
            expiration = expirationTime2
            secretKey = "testSecretKey2"
            accessKeyId = "testAccessKeyId2"
            sessionToken = "testSessionToken2"
        }

        val provider = CognitoCredentialsProvider("identityPool", "us-east-1")

        runBlocking {

            // Call resolve
            var credentials = provider.resolve()

            // Verify the returned credentials match what we expected
            assertEquals("testAccessKeyId", credentials.accessKeyId)
            assertEquals("testSessionToken", credentials.sessionToken)
            assertEquals(expirationTime, credentials.expiration)

            credentials = provider.resolve()

            // Verify the returned credentials match what we expected
            assertEquals("testAccessKeyId", credentials.accessKeyId)
            assertEquals("testSessionToken", credentials.sessionToken)
            assertEquals(expirationTime, credentials.expiration)
        }
    }
    @Test
    fun `If credentials are expired they will trigger a refresh`() {
        val expirationTime =
            Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds - 10000)
        val expirationTime2 =
            Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds + 20000)

        // Create mock CognitoIdentityClient
        mockkConstructor(CognitoCredentialsProvider::class)
        coEvery {
            anyConstructed<CognitoCredentialsProvider>().fetchCognitoCredentials()
        } returns Credentials.invoke {
            expiration = expirationTime
            secretKey = "testSecretKey"
            accessKeyId = "testAccessKeyId"
            sessionToken = "testSessionToken"
        } andThen Credentials.invoke {
            expiration = expirationTime2
            secretKey = "testSecretKey2"
            accessKeyId = "testAccessKeyId2"
            sessionToken = "testSessionToken2"
        }

        val provider = CognitoCredentialsProvider("identityPool", "us-east-1")

        runBlocking {

            // Call resolve
            var credentials = provider.resolve()

            // Verify the returned credentials match what we expected
            assertEquals("testAccessKeyId", credentials.accessKeyId)
            assertEquals("testSessionToken", credentials.sessionToken)
            assertEquals(expirationTime, credentials.expiration)

            credentials = provider.resolve()

            // Verify the returned credentials match what we expected
            assertEquals("testAccessKeyId2", credentials.accessKeyId)
            assertEquals("testSessionToken2", credentials.sessionToken)
            assertEquals(expirationTime2, credentials.expiration)
        }
    }
}
