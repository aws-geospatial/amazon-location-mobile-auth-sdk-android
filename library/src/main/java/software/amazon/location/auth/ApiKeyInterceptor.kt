// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.net.url.Url
import software.amazon.location.auth.utils.Constants.QUERY_PARAM_KEY

class ApiKeyInterceptor(
    private val apiKey: String,
) : HttpInterceptor {
    override suspend fun modifyBeforeSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        val req = context.protocolRequest.toBuilder()

        return if (!context.protocolRequest.url.toString().contains("$QUERY_PARAM_KEY=")) {
            req.url(Url.parse(context.protocolRequest.url.toString()+"?$QUERY_PARAM_KEY=$apiKey"))
            req.build()
        } else {
            super.modifyBeforeSigning(context)
        }
    }
}
