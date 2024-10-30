// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.location.auth.utils

enum class AwsRegions(val regionName: String) {
    GovCloud("us-gov-west-1"),
    US_GOV_EAST_1("us-gov-east-1"),
    US_EAST_1("us-east-1"),
    US_EAST_2("us-east-2"),
    US_WEST_1("us-west-1"),
    US_WEST_2("us-west-2"),
    EU_SOUTH_1("eu-south-1"),
    EU_SOUTH_2("eu-south-2"),
    EU_WEST_1("eu-west-1"),
    EU_WEST_2("eu-west-2"),
    EU_WEST_3("eu-west-3"),
    EU_CENTRAL_1("eu-central-1"),
    EU_CENTRAL_2("eu-central-2"),
    EU_NORTH_1("eu-north-1"),
    AP_EAST_1("ap-east-1"),
    AP_SOUTH_1("ap-south-1"),
    AP_SOUTHEAST_1("ap-southeast-1"),
    AP_SOUTHEAST_2("ap-southeast-2"),
    AP_SOUTHEAST_4("ap-southeast-4"),
    AP_NORTHEAST_1("ap-northeast-1"),
    AP_NORTHEAST_2("ap-northeast-2"),
    SA_EAST_1("sa-east-1"),
    CA_CENTRAL_1("ca-central-1"),
    CN_NORTH_1("cn-north-1"),
    CN_NORTHWEST_1("cn-northwest-1"),
    ME_SOUTH_1("me-south-1"),
    AF_SOUTH_1("af-south-1"),
    AP_SOUTHEAST_3("ap-southeast-3"),
    ME_CENTRAL_1("me-central-1"),
    AP_SOUTH_2("ap-south-2"),
    IL_CENTRAL_1("il-central-1");


    companion object {
        val DEFAULT_REGION: AwsRegions = US_WEST_2
        fun fromName(regionName: String): AwsRegions {
            return entries.find { it.regionName == regionName }
                ?: throw IllegalArgumentException("Cannot create enum from $regionName value!")
        }
    }
}
