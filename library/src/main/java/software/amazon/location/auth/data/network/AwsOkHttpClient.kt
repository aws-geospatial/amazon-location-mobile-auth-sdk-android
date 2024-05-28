package software.amazon.location.auth.data.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import software.amazon.location.auth.AwsSignerInterceptor
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.auth.utils.Constants.CONNECTION_TIMEOUT
import software.amazon.location.auth.utils.Constants.READ_TIMEOUT

internal class AwsOkHttpClient {

    companion object {
        /**
         * Creates and returns an OkHttpClient configured with AWS request signing.
         *
         * @param serviceName The name of the AWS service (e.g., "execute-api").
         * @param region The AWS region (e.g., "us-west-2").
         * @param credentialsProvider The provider for obtaining AWS credentials.
         * @return An OkHttpClient instance with AWS signing interceptor.
         */
        fun getClient(
            serviceName: String,
            region: String,
            credentialsProvider: LocationCredentialsProvider?
        ): OkHttpClient {
            val awsSignerInterceptor = AwsSignerInterceptor(serviceName, region, credentialsProvider)

            return OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(awsSignerInterceptor)
                .build()
        }
    }
}