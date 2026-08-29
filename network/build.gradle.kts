plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candiriya.quality")
}

dependencies {
    api(project(":config"))
    api(project(":servers"))
    api(project(":protocol"))
    api(project(":scheduler"))
    api(libs.nettyTransport)
    api(libs.nettyCodec)
    api(libs.nettyHandler)
    api(libs.nettyBuffer)
    api(libs.adventureApi)
    api(libs.adventureGson)
    api(libs.adventureMiniMessage)
    implementation(libs.log4jApi)
    implementation(libs.guava)
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
