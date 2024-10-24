pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri(file("${rootDir}/temp_clients/aws-sdk-kotlin-preview-maps/m2"))
    }
    maven {
      url = uri(file("${rootDir}/temp_clients/aws-sdk-kotlin-preview-places/m2"))
    }
    maven {
      url = uri(file("${rootDir}/temp_clients/aws-sdk-kotlin-preview-routes/m2"))
    }
  }
}
rootProject.name = "Amazon Location Auth SDK"
include("library")
