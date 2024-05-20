package software.amazon.location.auth.data.request

import com.google.gson.annotations.SerializedName

data class GetIdentityIdRequest(
    @SerializedName("IdentityPoolId")
    val identityPoolId: String
)