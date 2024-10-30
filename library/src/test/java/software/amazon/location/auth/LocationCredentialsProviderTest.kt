// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.sdk.kotlin.services.geomaps.GeoMapsClient
import aws.sdk.kotlin.services.geoplaces.GeoPlacesClient
import aws.sdk.kotlin.services.georoutes.GeoRoutesClient
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkConstructor
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.utils.AwsRegions
import software.amazon.location.auth.utils.Constants.TEST_IDENTITY_POOL_ID

class LocationCredentialsProviderTest {
    private lateinit var expirationTime: Instant

    @Before
    fun setUp() {
        expirationTime = Instant.fromEpochMilliseconds(Instant.now().epochMilliseconds + 10000) // 10 seconds in the future
    }

    @Test
    fun `Constructs successfully with identity pool and region`() {
        val mockCredentials =
            Credentials.invoke {
                expiration = expirationTime
                secretKey = "testSecretKey"
                accessKeyId = "testAccessKeyId"
                sessionToken = "testSessionToken"
            }
        mockkConstructor(CognitoCredentialsProvider::class)
        coEvery {
            anyConstructed<CognitoCredentialsProvider>().fetchCognitoCredentials()
        } returns mockCredentials

        val provider = LocationCredentialsProvider(TEST_IDENTITY_POOL_ID, AwsRegions.US_EAST_1)
        assertNotNull(provider)
        assertEquals(provider.getMethod(), "cognito")
        assertNull(provider.getApiKey())
        assertNotNull(provider.getCredentialsProvider())

        runBlocking {
            val credentials = provider.getCredentials()
            assertEquals(credentials.accessKeyId, mockCredentials.accessKeyId)
            assertEquals(credentials.secretAccessKey, mockCredentials.secretKey)
            assertEquals(credentials.sessionToken, mockCredentials.sessionToken)
        }
    }


    @Test
    fun `Constructs successfully with custom CredentialsProvider`() {
        val credentialsProvider = StaticCredentialsProvider(
            aws.smithy.kotlin.runtime.auth.awscredentials.Credentials.invoke(
                accessKeyId = "testAccessKey",
                secretAccessKey = "testSecretAccessKey",
                sessionToken = "testSessionToken",
                expiration = expirationTime
            ))
        val provider = LocationCredentialsProvider(credentialsProvider, AwsRegions.US_EAST_1)
        assertNotNull(provider)
        assertEquals(provider.getMethod(), "custom")
        assertNull(provider.getApiKey())
        assertNotNull(provider.getCredentialsProvider())

        runBlocking {
            val credentials = provider.getCredentials()
            assertEquals(credentials.accessKeyId, credentialsProvider.credentials.accessKeyId)
            assertEquals(credentials.secretAccessKey, credentialsProvider.credentials.secretAccessKey)
            assertEquals(credentials.sessionToken, credentialsProvider.credentials.sessionToken)
        }
    }

    @Test
    fun `Constructs successfully with ApiKey`() {
        val apiKey = "TestApiKey"
        val provider = LocationCredentialsProvider(AwsRegions.US_EAST_1, apiKey)
        assertNotNull(provider)
        assertEquals(provider.getMethod(), "apiKey")
        assertEquals(provider.getApiKey(), apiKey)
        assertNotNull(provider.getCredentialsProvider())

        runBlocking {
            val credentials = provider.getCredentials()
            assertEquals(credentials.accessKeyId, "")
            assertEquals(credentials.secretAccessKey, "")
            assertNull(credentials.sessionToken)
        }
    }

    @Test
    fun `getLocationClientConfig can be used to successfully construct a LocationClient`() {
        val apiKey = "TestApiKey"
        val provider = LocationCredentialsProvider(AwsRegions.US_EAST_1, apiKey)
        assertNotNull(provider.getLocationClientConfig())
    }

    fun `getGeoMapsClientConfig can be used to successfully construct a GeoMapsClient`() {
        val apiKey = "TestApiKey"
        val provider = LocationCredentialsProvider(AwsRegions.US_EAST_1, apiKey)
        assertNotNull(provider.getGeoMapsClientConfig())
    }

    fun `getGeoPlacesClientConfig can be used to successfully construct a GeoPlacesClient`() {
        val apiKey = "TestApiKey"
        val provider = LocationCredentialsProvider(AwsRegions.US_EAST_1, apiKey)
        assertNotNull(provider.getGeoPlacesClientConfig())
    }

    fun `getGeoRoutesClientConfig can be used to successfully construct a GeoRoutesClient`() {
        val apiKey = "TestApiKey"
        val provider = LocationCredentialsProvider(AwsRegions.US_EAST_1, apiKey)
        assertNotNull(provider.getGeoRoutesClientConfig())
    }

}
