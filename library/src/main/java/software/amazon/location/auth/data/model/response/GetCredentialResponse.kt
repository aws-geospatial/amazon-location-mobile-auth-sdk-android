package software.amazon.location.auth.data.model.response

import com.google.gson.annotations.SerializedName

data class GetCredentialResponse(
    @SerializedName("Credentials") val credentials: Credentials,
    @SerializedName("IdentityId") val identityId: String
)