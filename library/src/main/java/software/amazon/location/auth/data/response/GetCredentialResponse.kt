package software.amazon.location.auth.data.response

import com.google.gson.annotations.SerializedName



data class GetCredentialResponse(
    @SerializedName("Credentials") val credentials: Credentials,
    @SerializedName("IdentityId") val identityId: String
)