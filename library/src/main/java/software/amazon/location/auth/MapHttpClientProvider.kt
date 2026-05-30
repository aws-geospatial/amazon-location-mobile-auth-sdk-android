// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Provides a pre-configured OkHttpClient for use with MapLibre's HttpRequestUtil.
 * This client automatically attaches X-Android-Package and X-Android-Cert headers
 * to all map tile requests, enabling API key Android app restrictions.
 *
 * Usage:
 * ```
 * MapLibre.getInstance(this)
 * HttpRequestUtil.setOkHttpClient(MapHttpClientProvider.getMapHttpClient(this))
 * ```
 */
object MapHttpClientProvider {

    /**
     * Creates an OkHttpClient that attaches Android app identity headers to all requests.
     * Use with MapLibre's HttpRequestUtil.setOkHttpClient() to enable API key Android restrictions
     * for map tile requests.
     *
     * @param context The application context.
     * @return A configured OkHttpClient with the Android identity headers interceptor.
     */
    fun getMapHttpClient(context: Context): OkHttpClient {
        val identityProvider = DefaultAndroidAppIdentityProvider(context)
        return OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                requestBuilder.addHeader("X-Android-Package", identityProvider.packageName)
                identityProvider.certFingerprint?.let { fingerprint ->
                    requestBuilder.addHeader("X-Android-Cert", fingerprint)
                }
                chain.proceed(requestBuilder.build())
            })
            .build()
    }
}
