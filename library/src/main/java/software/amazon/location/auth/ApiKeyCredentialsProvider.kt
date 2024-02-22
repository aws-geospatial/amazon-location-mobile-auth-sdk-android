package software.amazon.location.auth

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore

private const val API_KEY = "apiKey"

/**
 * Provides API key credentials for accessing services and manages their storage.
 */
class ApiKeyCredentialsProvider {
    private var awsKeyValueStore: AWSKeyValueStore? = null

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
        awsKeyValueStore = AWSKeyValueStore(
            context, "software.amazon.location.auth", true
        )
    }

    private fun saveCredentials(apiKey: String) {
        if (awsKeyValueStore === null) throw Exception("Not initialized")
        awsKeyValueStore!!.put(API_KEY, apiKey)
    }

    /**
     * Retrieves the cached API key credentials.
     * @return The API key or null if not found.
     * @throws Exception If the AWSKeyValueStore is not initialized.
     */
    fun getCachedCredentials(): String? {
        if (awsKeyValueStore === null) throw Exception("Not initialized")
        return awsKeyValueStore!!.get(API_KEY)
    }

    /**
     * Clears the stored credentials.
     */
    fun clearCredentials() {
        awsKeyValueStore?.clear()
    }
}