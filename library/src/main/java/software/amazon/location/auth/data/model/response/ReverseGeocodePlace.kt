package software.amazon.location.auth.data.model.response

import com.google.gson.annotations.SerializedName

data class ReverseGeocodePlace(
    @SerializedName("Place")
    val place: ReverseGeocodeLabel
)