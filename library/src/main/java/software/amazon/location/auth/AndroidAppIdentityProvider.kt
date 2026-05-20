// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

/**
 * Provides Android application identity for API key request headers.
 */
interface AndroidAppIdentityProvider {
    val packageName: String
    val certFingerprint: String?
}
