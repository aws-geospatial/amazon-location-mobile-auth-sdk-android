package software.amazon.location.auth


import aws.sdk.kotlin.services.location.LocationClient
import aws.sdk.kotlin.services.location.model.SearchPlaceIndexForPositionRequest
import aws.sdk.kotlin.services.location.model.SearchPlaceIndexForPositionResponse

/**
 * Provides methods to interact with the Amazon Location service.
 *
 * @property locationClient An instance of LocationClient used for making requests to the Amazon Location service.
 */
class AmazonLocationClient(
    private val locationClient: LocationClient
) {

    /**
     * Reverse geocodes a location specified by longitude and latitude coordinates.
     *
     * @param placeIndexName The name of the place index resource to use for the reverse geocoding request.
     * @param longitude The longitude of the location to reverse geocode.
     * @param latitude The latitude of the location to reverse geocode.
     * @param mLanguage The language to use for the reverse geocoding results.
     * @param mMaxResults The maximum number of results to return.
     * @return A response containing the reverse geocoding results.
     */
    suspend fun reverseGeocode(
        placeIndexName: String,
        longitude: Double,
        latitude: Double,
        mLanguage: String,
        mMaxResults: Int
    ): SearchPlaceIndexForPositionResponse {
        val request = SearchPlaceIndexForPositionRequest {
            indexName = placeIndexName
            position = listOf(longitude, latitude)
            maxResults = mMaxResults
            language = mLanguage
        }

        val response = locationClient.searchPlaceIndexForPosition(request)
        return response
    }
}
