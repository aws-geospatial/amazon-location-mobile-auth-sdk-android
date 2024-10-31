package software.amazon.location.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Wrapper class for encrypted SharedPreferences.
 * Provides methods to initialize, store, retrieve, and clear encrypted preferences.
 *
 * @property context The application context.
 * @property preferenceName The name of the preferences file.
 */
class EncryptedSharedPreferences(private val context: Context, private val preferenceName: String) {
    private var sharedPreferences: SharedPreferences? = null

    /**
     * Initializes the EncryptedSharedPreferences instance.
     *
     * @throws RuntimeException if initialization fails due to security or I/O issues.
     */
    fun initEncryptedSharedPreferences() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                preferenceName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Failed to initialize encrypted preferences", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to initialize encrypted preferences", e)
        }
    }

    /**
     * Stores a string value in the encrypted preferences.
     * @param key The key under which to store the value.
     * @param value The value to store.
     * @throws Exception if preferences are not initialized.
     */
    fun put(key: String, value: String) {
        if (sharedPreferences === null) throw Exception("SharedPreferences not initialized")
        val editor = sharedPreferences!!.edit()
        editor.putString(key, value)
        editor.apply()
    }

    /**
     * Retrieves a string value from the encrypted preferences.
     * @param key The key of the value to retrieve.
     * @return The retrieved value, or null if the key does not exist.
     * @throws Exception if preferences are not initialized.
     */
    fun get(key: String): String? {
        if (sharedPreferences === null) throw Exception("SharedPreferences not initialized")
        return sharedPreferences!!.getString(key, null)
    }

    /**
     * Clears all entries from the encrypted preferences.
     * @throws Exception if preferences are not initialized.
     */
    fun clear() {
        if (sharedPreferences === null) throw Exception("SharedPreferences not initialized")
        sharedPreferences!!.edit().clear().apply()
    }

    /**
     * Removes a particular key from the encrypted preferences.
     * @param key The key to remove.
     * @throws Exception if preferences are not initialized.
     */
    fun remove(key: String) {
        if (sharedPreferences === null) throw Exception("SharedPreferences not initialized")
        val editor = sharedPreferences!!.edit()
        editor.remove(key)
        editor.apply()
    }

    /**
     * Checks if a particular key exists in the encrypted preferences.
     * @param key The key to check for existence.
     * @return True if the key exists, false otherwise.
     * @throws Exception if preferences are not initialized.
     */
    fun contains(key: String): Boolean {
        if (sharedPreferences === null) throw Exception("SharedPreferences not initialized")
        return sharedPreferences!!.contains(key)
    }
}
