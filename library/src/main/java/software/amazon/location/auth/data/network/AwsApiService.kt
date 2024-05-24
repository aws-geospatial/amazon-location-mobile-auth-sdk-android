package software.amazon.location.auth.data.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import software.amazon.location.auth.data.model.request.ReverseGeocodeRequest
import software.amazon.location.auth.data.model.response.ReverseGeocodeResponse

interface AwsApiService {
    @POST("places/v0/indexes/{indexName}/search/position")
    @Headers("Content-Type: application/json")
    suspend fun reverseGeocode(
        @Path("indexName") indexName: String,
        @Body request: ReverseGeocodeRequest
    ): ReverseGeocodeResponse
}
