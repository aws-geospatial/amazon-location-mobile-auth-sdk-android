import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish") version "0.27.0"
}

publishing {
    repositories {
        maven {
            name = "AuthSDK"
            url = uri("https://aws.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials(PasswordCredentials::class)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.DEFAULT, automaticRelease = true)
    signAllPublications()

    coordinates("software.amazon.location", "auth", "0.2.2")

    pom {
        name.set("Amazon Location Service Mobile Authentication SDK for Android")
        description.set("These utilities help you authenticate when making Amazon Location Service API calls from your Android applications.")
        inceptionYear.set("2024")
        url.set("https://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("aws-geospatial")
                name.set("AWS Geospatial")
                url.set("https://github.com/aws-geospatial")
            }
        }
        scm {
            url.set("https://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android")
            connection.set("scm:git:git://github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android")
            developerConnection.set("scm:git:ssh://git@github.com/aws-geospatial/amazon-location-mobile-auth-sdk-android")
        }
    }
}

android {

    namespace = "software.amazon.location.auth"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("aws.sdk.kotlin:cognitoidentity:1.2.21")
    implementation("aws.sdk.kotlin:location:1.2.21")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("io.mockk:mockk:1.13.10")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
