package software.amazon.location.auth.data.model.response

import com.google.gson.annotations.SerializedName

data class GetIdentityIdResponse(
    @SerializedName("IdentityId") val identityId: String
)