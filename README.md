# Amazon Location Service Mobile Authentication SDK for Android

These utilities help you authenticate when making [Amazon Location Service](https://aws.amazon.com/location/) API calls from your Android applications. This specifically helps when using [Amazon Cognito](https://docs.aws.amazon.com/location/latest/developerguide/authenticating-using-cognito.html) as the authentication method.

## Installation

This authentication SDK works with the overall AWS Kotlin SDK. Both SDKs are published to Maven Central.
Check the [latest version](https://mvnrepository.com/artifact/software.amazon.location/auth) of auth
SDK on Maven Central.

Add the following lines to the dependencies section of your build.gradle file in Android Studio:

```
implementation("software.amazon.location:auth:0.2.5")
implementation("aws.sdk.kotlin:location:1.2.21")
implementation("org.maplibre.gl:android-sdk:11.0.0-pre5")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

## Usage

Import the following classes in your code:

```
import aws.sdk.kotlin.services.location.LocationClient

import software.amazon.location.auth.AuthHelper
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.auth.AwsSignerInterceptor
import org.maplibre.android.module.http.HttpRequestUtil
import okhttp3.OkHttpClient
```

You can create an AuthHelper and use it with the AWS Kotlin SDK:

```
// Create a credentail provider using Identity Pool Id with AuthHelper
private fun exampleCognitoLogin() {
    var authHelper = AuthHelper(applicationContext)
    var locationCredentialsProvider : LocationCredentialsProvider = authHelper.authenticateWithCognitoIdentityPool("My-Cognito-Identity-Pool-Id")
    var locationClient = locationCredentialsProvider?.getLocationClient()
}

OR

// Create a credentail provider using custom credential provider with AuthHelper
private fun exampleCustomCredentialLogin() {
    var authHelper = AuthHelper(applicationContext)
    var locationCredentialsProvider : LocationCredentialsProvider = authHelper.authenticateWithCredentialsProvider("MY-AWS-REGION", MY-CUSTOM-CREDENTIAL-PROVIDER)
    var locationClient = locationCredentialsProvider?.getLocationClient()
}
```
You can use the LocationCredentialsProvider to load the maplibre map. Here is an example of that:

```
HttpRequestUtil.setOkHttpClient(
    OkHttpClient.Builder()
        .addInterceptor(
            AwsSignerInterceptor(
                "geo",
                "MY-AWS-REGION",
                locationCredentialsProvider
            )
        )
        .build()
)
```

You can use the LocationClient to make calls to Amazon Location Service. Here is an example that searches for places near a specified latitude and longitude:

```
val searchPlaceIndexForPositionRequest = SearchPlaceIndexForPositionRequest {
       indexName = "My-Place-Index-Name"
       position = listOf(30.405423, -97.718833)
       maxResults = MAX_RESULT
       language = "PREFERRED-LANGUAGE"
   }
val nearbyPlaces = locationClient.searchPlaceIndexForPosition(request)
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
