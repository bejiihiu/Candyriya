plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candiriya.quality")
}

dependencies {
    api(project(":config"))
    api(libs.nettyTransport)
    api(libs.nettyCodec)
    api(libs.nettyHandler)
    api(libs.nettyBuffer)
    implementation(libs.log4jApi)
    implementation(libs.guava)
    compileOnly(libs.checkerQual)

    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)
    testImplementation(libs.assertj)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testRuntimeOnly(libs.log4jCore)
}
