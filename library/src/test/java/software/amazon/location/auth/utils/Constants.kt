package software.amazon.location.auth.utils

object Constants {
    val JSON =
        "{\"IdentityId\":\"us-east-1:xxxxxx-xxxx-xxxx-xxxxx-xxxxxxxxxx\"}"
    val JSON_2 =
        "{\"Credentials\":{\"AccessKeyId\":\"test\",\"Expiration\":1.715700986E9,\"SecretKey\":\"test\",\"SessionToken\":\"test\"},\"IdentityId\":\"us-east-1:xxxxxx-xxxx-xxxx-xxxxx-xxxxxxxxxx\"}"
    const val ACCESS_KEY_ID= "accessKeyId"
    const val SECRET_KEY= "secretKey"
    const val SESSION_TOKEN= "sessionToken"
    const val EXPIRATION= "expiration"
}