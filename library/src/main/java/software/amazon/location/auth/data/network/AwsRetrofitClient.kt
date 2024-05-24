package software.amazon.location.auth.data.network

import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.auth.utils.Constants.RESPONSE_CODE_CREDENTIAL_EXPIRED

/**
 * A singleton object that manages the Retrofit client and API service for AWS requests.
 */
object AwsRetrofitClient {
    private lateinit var retrofit: Retrofit
    private var _apiService: AwsApiService? = null

    /**
     * Initializes the Retrofit client with the given parameters.
     *
     * @param baseUrl The base URL for the Retrofit client.
     * @param serviceName The name of the AWS service (e.g., "execute-api").
     * @param region The AWS region (e.g., "us-west-2").
     * @param credentialsProvider The provider for obtaining AWS credentials.
     */
    fun init(baseUrl: String, serviceName: String, region: String, credentialsProvider: LocationCredentialsProvider?) {
        val client = AwsOkHttpClient.getClient(serviceName, region, credentialsProvider)
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Retrieves the API service instance. If it doesn't exist, it will be created.
     *
     * @return The API service instance.
     */
    val apiService: AwsApiService
        get() {
            if (_apiService == null) {
                createApiService()
            }
            return _apiService!!
        }

    /**
     * Creates the API service instance using the Retrofit client.
     */
    private fun createApiService() {
        _apiService = retrofit.create(AwsApiService::class.java)
    }

    /**
     * Clears the current API service instance, forcing it to be recreated on the next access.
     */
    @Synchronized
    fun clearApiService() {
        _apiService = null
    }

    /**
     * Checks if the given exception corresponds to an expired credentials error.
     *
     * @param e The exception to check.
     * @return True if the exception is a HttpException with a status code indicating expired credentials, false otherwise.
     */
    fun isHttpStatusCodeCredentialExpired(e: Exception): Boolean {
        if (e is HttpException) {
            return e.code() == RESPONSE_CODE_CREDENTIAL_EXPIRED
        }
        return false
    }
}