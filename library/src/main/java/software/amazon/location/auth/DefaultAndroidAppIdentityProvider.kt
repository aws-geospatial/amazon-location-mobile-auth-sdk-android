// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Default implementation that reads package name and signing certificate from Android Context.
 */
class DefaultAndroidAppIdentityProvider(private val context: Context) : AndroidAppIdentityProvider {

    override val packageName: String
        get() = context.packageName

    override val certFingerprint: String? by lazy { getSigningCertFingerprint() }

    private fun getSigningCertFingerprint(): String? {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val signature = signatures?.firstOrNull() ?: return null
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(signature.toByteArray())
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
