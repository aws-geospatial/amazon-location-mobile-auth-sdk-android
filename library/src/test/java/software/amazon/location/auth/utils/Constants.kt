package software.amazon.location.auth.utils

import io.mockk.impl.recording.WasNotCalled.method

object Constants {
    const val ACCESS_KEY_ID = "accessKeyId"
    const val SECRET_KEY = "secretKey"
    const val SESSION_TOKEN = "sessionToken"
    const val EXPIRATION = "expiration"
    const val METHOD = "method"
    const val IDENTITY_POOL_ID = "identityPoolId"
    const val API_KEY = "apiKey"
    const val REGION = "region"
    const val HEADER_HOST = "Host"
    const val HEADER_X_AMZ_SECURITY_TOKEN = "x-amz-security-token"
    const val HEADER_AUTHORIZATION = "Authorization"
    const val TEST_REGION = "us-west-2"
    const val TEST_URL = "https://service.amazonaws.com"
    const val TEST_URL1 = "https://example.com"
    const val TEST_IDENTITY_POOL_ID = "us-east-1:dummyIdentityPoolId"
}