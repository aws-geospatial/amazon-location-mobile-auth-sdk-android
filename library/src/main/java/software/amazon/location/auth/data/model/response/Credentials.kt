package software.amazon.location.auth.data.model.response

import com.google.gson.annotations.SerializedName

data class Credentials(
    @SerializedName("AccessKeyId") val accessKeyId: String,
    @SerializedName("Expiration") val expiration: Double,
    @SerializedName("SecretKey") val secretKey: String,
    @SerializedName("SessionToken") val sessionToken: String
)