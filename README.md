# Amazon Location Service Mobile Authentication SDK for Android

These utilities help you authenticate when making [Amazon Location Service](https://aws.amazon.com/location/) API calls from your Android applications. This specifically helps when using [Amazon Cognito](https://docs.aws.amazon.com/location/latest/developerguide/authenticating-using-cognito.html) as the authentication method.

## Installation

This authentication SDK works with the overall AWS Kotlin SDK. Both SDKs are published to Maven Central.
Check the [latest version](https://mvnrepository.com/artifact/software.amazon.location/auth) of auth
SDK on Maven Central.

Add the following lines to the dependencies section of your build.gradle file in Android Studio:

```
implementation("software.amazon.location:auth:1.0.0")
implementation("org.maplibre.gl:android-sdk:11.5.2")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

For the new standalone Maps / Places / Routes SDKs, add the following lines:
```
implementation("aws.sdk.kotlin:geomaps:1.3.65")
implementation("aws.sdk.kotlin:geoplaces:1.3.65")
implementation("aws.sdk.kotlin:georoutes:1.3.65")
```

For the consolidated Location SDK that includes Geofencing and Tracking, add the following line:
```
implementation("aws.sdk.kotlin:location:1.3.65")
```

## Usage

Import the following classes in your code:

```
// For the standalone Maps / Places / Routes SDKs
import aws.sdk.kotlin.services.geomaps.GeoMapsClient
import aws.sdk.kotlin.services.geoplaces.GeoPlacesClient
import aws.sdk.kotlin.services.georoutes.GeoRoutesClient

// For the consolidated Location SDK
import aws.sdk.kotlin.services.location.LocationClient

import software.amazon.location.auth.AuthHelper
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.auth.AwsSignerInterceptor
import org.maplibre.android.module.http.HttpRequestUtil
import okhttp3.OkHttpClient
```

You can create an AuthHelper and use it with the AWS Kotlin SDK:

```
// Create a credential provider using Identity Pool Id with AuthHelper
private suspend fun exampleCognitoLogin() {
    val authHelper = AuthHelper.withCognitoIdentityPool("MY-COGNITO-IDENTITY-POOL-ID")
    
    // Get instances of the standalone clients:
    var geoMapsClient = GeoMapsClient(authHelper?.getGeoMapsClientConfig())
    var geoPlacesClient = GeoPlacesClient(authHelper?.getGeoPlacesClientConfig())
    var geoRoutesClient = GeoRoutesClient(authHelper?.getGeoRoutesClientConfig())
    
    // Get an instance of the Location client:
    var locationClient = LocationClient(authHelper?.getLocationClientConfig())
}

OR

// Create a credential provider using custom credential provider with AuthHelper
private suspend fun exampleCustomCredentialLogin() {
    var authHelper = AuthHelper.withCredentialsProvider(MY-CUSTOM-CREDENTIAL-PROVIDER, "MY-AWS-REGION")

    // Get instances of the standalone clients:
    var geoMapsClient = GeoMapsClient(authHelper?.getGeoMapsClientConfig())
    var geoPlacesClient = GeoPlacesClient(authHelper?.getGeoPlacesClientConfig())
    var geoRoutesClient = GeoRoutesClient(authHelper?.getGeoRoutesClientConfig())
    
    // Get an instance of the Location client:
    var locationClient = LocationClient(authHelper?.getLocationClientConfig())

OR

// Create a credential provider using Api key with AuthHelper
private suspend fun exampleApiKeyLogin() {
    var authHelper = AuthHelper.withApiKey("MY-API-KEY", "MY-AWS-REGION")

    // Get instances of the standalone clients:
    var geoMapsClient = GeoMapsClient(authHelper?.getGeoMapsClientConfig())
    var geoPlacesClient = GeoPlacesClient(authHelper?.getGeoPlacesClientConfig())
    var geoRoutesClient = GeoRoutesClient(authHelper?.getGeoRoutesClientConfig())
    
    // Get an instance of the Location client:
    var locationClient = LocationClient(authHelper?.getLocationClientConfig())
}
```
You can use the LocationCredentialsProvider to load the maplibre map. Here is an example of that:

```
HttpRequestUtil.setOkHttpClient(
    OkHttpClient.Builder()
        .addInterceptor(
            AwsSignerInterceptor(
                applicationContext,
                "geo",
                "MY-AWS-REGION",
                locationCredentialsProvider
            )
        )
        .build()
)
```

You can use the created clients to make calls to Amazon Location Service. Here is an example that searches for places near a specified latitude and longitude:

```
val suggestRequest = SuggestRequest {
       biasPosition = listOf(-97.718833, 30.405423)
       maxResults = MAX_RESULT
       language = "PREFERRED-LANGUAGE"
   }
val nearbyPlaces = geoPlacesClient.suggest(request)
```

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## Getting Help

The best way to interact with our team is through GitHub.
You can [open an issue](https://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android/issues/new/choose) and choose from one of our templates for
[bug reports](https://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android/issues/new?assignees=&labels=bug%2C+needs-triage&template=---bug-report.md&title=),
[feature requests](https://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android/issues/new?assignees=&labels=feature-request&template=---feature-request.md&title=)
or [guidance](https://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android/issues/new?assignees=&labels=guidance%2C+needs-triage&template=---questions---help.md&title=).
If you have a support plan with [AWS Support](https://aws.amazon.com/premiumsupport/), you can also create a new support case.

## Contributing

We welcome community contributions and pull requests. See [CONTRIBUTING.md](https://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android/blob/master/CONTRIBUTING.md) for information on how to set up a development environment and submit code.

## License

The Amazon Location Service Mobile Authentication SDK for Android is distributed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0),
see LICENSE.txt and NOTICE.txt for more information.
