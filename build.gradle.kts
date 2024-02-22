plugins {
  id("org.jetbrains.kotlin.android") version "1.9.0" apply false
  id("com.android.library") version "8.2.2" apply false
  id("com.gradleup.nmcp") version "0.0.4"
}

nmcp {
  publishAllProjectsProbablyBreakingProjectIsolation {
    username = findProperty("mavenCentralUsername").toString()
    password = findProperty("mavenCentralPassword").toString()
    publicationType = "AUTOMATIC"
  }
}