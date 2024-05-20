package software.amazon.location.auth.utils


object Constants {
    const val URL = "https://cognito-identity.%s.amazonaws.com/"
    const val MEDIA_TYPE = "application/x-amz-json-1.1"
    const val HEADER_X_AMZ_TARGET = "X-Amz-Target"
    const val TIME_PATTERN = "yyyyMMdd'T'HHmmss'Z'"
    const val HEADER_X_AMZ_DATE = "x-amz-date"
    const val HEADER_HOST = "host"
    const val HEADER_X_AMZ_SECURITY_TOKEN = "x-amz-security-token"
    const val HEADER_X_AMZ_CONTENT_SHA256= "x-amz-content-sha256"
    const val HEADER_AUTHORIZATION= "authorization"
    const val ACCESS_KEY_ID= "accessKeyId"
    const val SECRET_KEY= "secretKey"
    const val SESSION_TOKEN= "sessionToken"
    const val EXPIRATION= "expiration"
    const val REGION= "region"
    const val METHOD= "method"
    const val IDENTITY_POOL_ID= "identityPoolId"
    const val API_KEY = "apiKey"
}