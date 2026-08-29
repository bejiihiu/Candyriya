plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candyriya.quality")
}

spotbugs {
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
    ignoreFailures.set(true)
}

dependencies {
    api(project(":config"))
    api(libs.adventureApi)
    api(libs.adventureMiniMessage)
    api(libs.adventureGson)
    api(libs.guava)
    api(libs.kotlinxSerialization)
    api(libs.kotlinxCoroutines)
    implementation(libs.log4jApi)
    compileOnly(libs.checkerQual)
    compileOnly(libs.spotbugsAnnotations)

    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)
    testImplementation(libs.assertj)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testRuntimeOnly(libs.log4jCore)
}

