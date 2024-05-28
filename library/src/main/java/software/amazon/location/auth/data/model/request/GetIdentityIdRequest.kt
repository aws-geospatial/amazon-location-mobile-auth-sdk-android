package software.amazon.location.auth.data.model.request

import com.google.gson.annotations.SerializedName

data class GetIdentityIdRequest(
    @SerializedName("IdentityPoolId")
    val identityPoolId: String
)