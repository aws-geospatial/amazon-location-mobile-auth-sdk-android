package software.amazon.location.auth.data.model.response

import com.google.gson.annotations.SerializedName

data class ReverseGeocodeResponse(
    @SerializedName("Results")
    val results: List<ReverseGeocodePlace>
)
