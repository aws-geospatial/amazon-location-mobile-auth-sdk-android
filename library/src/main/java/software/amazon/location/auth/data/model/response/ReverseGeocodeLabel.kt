package software.amazon.location.auth.data.model.response

import com.google.gson.annotations.SerializedName

data class ReverseGeocodeLabel(
    @SerializedName("Label")
    val label: String
)