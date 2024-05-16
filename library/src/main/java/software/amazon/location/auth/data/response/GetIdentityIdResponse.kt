package software.amazon.location.auth.data.response

import com.google.gson.annotations.SerializedName

data class GetIdentityIdResponse(
    @SerializedName("IdentityId") val identityId: String
)