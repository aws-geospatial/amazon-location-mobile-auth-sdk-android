package software.amazon.location.auth

import android.content.Context
import software.amazon.location.auth.utils.Constants.API_KEY

/**
 * Provides API key credentials for accessing services and manages their storage.
 */
class ApiKeyCredentialsProvider {
    private var securePreferences: EncryptedSharedPreferences? = null

    /**
     * Initializes the provider and saves the provided API key.
     * @param context The application context.
     * @param apiKey The API key to save.
     */
    constructor(context: Context, apiKey: String) {
        initialize(context)
        saveCredentials(apiKey)
    }

    /**
     * Initializes the provider and retrieves the API key from the cache.
     * @param context The application context.
     * @throws Exception If no credentials are found in the cache.
     */
    constructor(context: Context) {
        initialize(context)
        val apiKey = getCachedCredentials()
        if (apiKey === null) throw Exception("No credentials found")
    }

    private fun initialize(context: Context) {
        securePreferences = EncryptedSharedPreferences(context, PREFS_NAME)
        securePreferences?.initEncryptedSharedPreferences()
    }

    private fun saveCredentials(apiKey: String) {
        if (securePreferences === null) throw Exception("Not initialized")
        securePreferences!!.put(API_KEY, apiKey)
    }

    /**
     * Retrieves the cached API key credentials.
     * @return The API key or null if not found.
     * @throws Exception If the AWSKeyValueStore is not initialized.
     */
    fun getCachedCredentials(): String? {
        if (securePreferences === null) throw Exception("Not initialized")
        return securePreferences!!.get(API_KEY)
    }

    /**
     * Clears the stored credentials.
     */
    fun clearCredentials() {
        securePreferences?.remove(API_KEY)
    }
}