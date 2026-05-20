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
import kotlin.test.assertNull
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

        val resultRequest = apiKeyInterceptor.modifyBeforeSigning(mockContext)

        assertEquals("$TEST_URL1?key=existingKey", resultRequest.url.toString())
    }

    @Test
    fun `test Android headers are set when identity provider is provided`() = runBlocking {
        val mockProvider = mockk<AndroidAppIdentityProvider>()
        every { mockProvider.packageName } returns "com.example.testapp"
        every { mockProvider.certFingerprint } returns "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD"

        val interceptor = ApiKeyInterceptor(apiKey, mockProvider)

        val mockContext = mockk<ProtocolRequestInterceptorContext<Any, HttpRequest>>(relaxed = true)
        val httpRequest = HttpRequest(method = HttpMethod.POST, url = Url.parse(TEST_URL1))
        every { mockContext.protocolRequest } returns httpRequest

        val resultRequest = interceptor.modifyBeforeSigning(mockContext)

        assertEquals("com.example.testapp", resultRequest.headers["X-Android-Package"])
        assertEquals("AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD", resultRequest.headers["X-Android-Cert"])
    }

    @Test
    fun `test no Android headers when identity provider is null`() = runBlocking {
        val interceptor = ApiKeyInterceptor(apiKey)

        val mockContext = mockk<ProtocolRequestInterceptorContext<Any, HttpRequest>>(relaxed = true)
        val httpRequest = HttpRequest(method = HttpMethod.POST, url = Url.parse(TEST_URL1))
        every { mockContext.protocolRequest } returns httpRequest

        val resultRequest = interceptor.modifyBeforeSigning(mockContext)

        assertNull(resultRequest.headers["X-Android-Package"])
        assertNull(resultRequest.headers["X-Android-Cert"])
    }

    @Test
    fun `test cert header not set when fingerprint is null`() = runBlocking {
        val mockProvider = mockk<AndroidAppIdentityProvider>()
        every { mockProvider.packageName } returns "com.example.testapp"
        every { mockProvider.certFingerprint } returns null

        val interceptor = ApiKeyInterceptor(apiKey, mockProvider)

        val mockContext = mockk<ProtocolRequestInterceptorContext<Any, HttpRequest>>(relaxed = true)
        val httpRequest = HttpRequest(method = HttpMethod.POST, url = Url.parse(TEST_URL1))
        every { mockContext.protocolRequest } returns httpRequest

        val resultRequest = interceptor.modifyBeforeSigning(mockContext)

        assertEquals("com.example.testapp", resultRequest.headers["X-Android-Package"])
        assertNull(resultRequest.headers["X-Android-Cert"])
    }
}
