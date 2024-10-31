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

    coordinates("software.amazon.location", "auth", "1.0.0")

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
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.security.crypto)
    implementation(libs.cognitoidentity)
    implementation(libs.location)
    implementation(libs.geomaps)
    implementation(libs.geoplaces)
    implementation(libs.georoutes)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
