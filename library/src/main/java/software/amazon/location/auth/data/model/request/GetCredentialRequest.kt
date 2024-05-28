package software.amazon.location.auth.data.model.request

import com.google.gson.annotations.SerializedName

data class GetCredentialRequest(
    @SerializedName("IdentityId")
    val identityId: String
)