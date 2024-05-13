package software.amazon.location.auth.data.request

import com.google.gson.annotations.SerializedName

data class GetCredentialRequest(
    @SerializedName("IdentityId")
    val identityId: String
)