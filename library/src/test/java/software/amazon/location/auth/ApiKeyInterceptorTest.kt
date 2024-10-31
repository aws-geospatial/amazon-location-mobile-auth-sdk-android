// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.utils.Constants.TEST_URL1

class ApiKeyInterceptorTest {

    private lateinit var apiKeyInterceptor: ApiKeyInterceptor
    private val apiKey = "testApiKey"

    @Before
    fun setup() {
        apiKeyInterceptor = ApiKeyInterceptor(apiKey)
    }

    @Test
    fun `test URL is modified when key is missing from the URL`() = runBlocking {
        val mockContext = mockk<ProtocolRequestInterceptorContext<Any, HttpRequest>>(relaxed = true)
        val httpRequest = HttpRequest(
            method= HttpMethod.POST,
            url= Url.parse(TEST_URL1),
        )

        every { mockContext.protocolRequest } returns httpRequest

        val resultRequest = apiKeyInterceptor.modifyBeforeSigning(mockContext)

        assertEquals("$TEST_URL1?key=testApiKey", resultRequest.url.toString())
    }

    @Test
    fun `test URL is not modified when key is present in the URL`() = runBlocking {
        val mockContext = mockk<ProtocolRequestInterceptorContext<Any, HttpRequest>>(relaxed = true)
        val httpRequest = HttpRequest(
            method= HttpMethod.POST,
            url= Url.parse("$TEST_URL1?key=existingKey"),
        )

        every { mockContext.protocolRequest } returns httpRequest

        // Act
        val resultRequest = apiKeyInterceptor.modifyBeforeSigning(mockContext)

        // Assert
        assertEquals("$TEST_URL1?key=existingKey", resultRequest.url.toString())
    }
}
