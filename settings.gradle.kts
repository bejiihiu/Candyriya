dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core")
include(":config")
include(":network")
include(":protocol")
include(":launcher")
include(":scheduler")

rootProject.name = "candiriya"
