package software.amazon.location.auth

import android.content.Context
import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import software.amazon.location.auth.utils.Constants.ACCESS_KEY_ID
import software.amazon.location.auth.utils.Constants.EXPIRATION
import software.amazon.location.auth.utils.Constants.SECRET_KEY
import software.amazon.location.auth.utils.Constants.SESSION_TOKEN

/**
 * Provides Cognito credentials for accessing services and manages their storage.
 */
class CognitoCredentialsProvider {
    private var securePreferences: EncryptedSharedPreferences? =null

    /**
     * Constructor that initializes the provider with a context and credentials.
     * @param context The application context used to initialize the key-value store.
     * @param credentials The credentials to be saved in the key-value store.
     */
    constructor(context: Context, credentials: Credentials) {
        initialize(context)
        saveCredentials(credentials)
    }

    /**
     * Constructor that initializes the provider with a context and retrieves cached credentials.
     * Throws an exception if no cached credentials are found.
     * @param context The application context used to initialize the key-value store.
     */
    constructor(context: Context) {
        initialize(context)
        val credentials = getCachedCredentials()
        if (credentials === null) throw Exception("No credentials found")
    }

    /**
     * Initializes the AWSKeyValueStore with the given context.
     * @param context The application context used to initialize the key-value store.
     */
    private fun initialize(context: Context) {
        securePreferences = EncryptedSharedPreferences(context, PREFS_NAME)
        securePreferences?.initEncryptedSharedPreferences()
    }

    /**
     * Saves the given credentials to the key-value store.
     * @param credentials The credentials to be saved in the key-value store.
     * @throws Exception if the key-value store is not initialized.
     */
    private fun saveCredentials(credentials: Credentials) {
        if (securePreferences === null) throw Exception("Not initialized")
        credentials.accessKeyId?.let { securePreferences?.put(ACCESS_KEY_ID, it) }
        credentials.secretKey?.let { securePreferences?.put(SECRET_KEY, it) }
        credentials.sessionToken?.let { securePreferences?.put(SESSION_TOKEN, it) }
        credentials.expiration?.let { securePreferences?.put(EXPIRATION, it.epochMilliseconds.toString()) }

    }

    /**
     * Retrieves cached credentials from the key-value store.
     * @return A Credentials object if all required fields are present, otherwise null.
     */
    fun getCachedCredentials(): Credentials? {
        if (securePreferences === null) return null
        val mAccessKeyId = securePreferences?.get(ACCESS_KEY_ID)
        val mSecretKey = securePreferences?.get(SECRET_KEY)
        val mSessionToken = securePreferences?.get(SESSION_TOKEN)
        val mExpiration = securePreferences?.get(EXPIRATION)
        if (mAccessKeyId.isNullOrEmpty() || mSecretKey.isNullOrEmpty() || mSessionToken.isNullOrEmpty() || mExpiration.isNullOrEmpty()) return null
        return Credentials.invoke {
            accessKeyId = mAccessKeyId
            secretKey = mSecretKey
            sessionToken = mSessionToken
            expiration = Instant.fromEpochMilliseconds(mExpiration.toLong())
        }
    }

    /**
     * Clears all credentials from the key-value store.
     */
    fun clearCredentials() {
        if (securePreferences === null) throw Exception("Not initialized")
        securePreferences?.remove(ACCESS_KEY_ID)
        securePreferences?.remove(SECRET_KEY)
        securePreferences?.remove(SESSION_TOKEN)
        securePreferences?.remove(EXPIRATION)
    }
}