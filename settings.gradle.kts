pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Quadrinheiro"

include(":app")
include(":macrobenchmark")

// Explicitly opt in to the isolated hardware probe; never bundled in the production APK.
if (providers.gradleProperty("includeNowBarProbe").orNull == "true") {
    include(":nowbar-probe")
    project(":nowbar-probe").projectDir = file("tools/nowbar-probe")
}
