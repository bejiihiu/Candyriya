plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candiriya.quality")
}

dependencies {
    api(project(":config"))
    implementation(libs.log4jApi)
    implementation(libs.guava)
    implementation(libs.kotlinxCoroutines)
    // netty for ThreadController.createBossGroup / createWorkerGroup - platform threads with proper naming
    implementation(libs.nettyTransport)
    compileOnly(libs.checkerQual)
    compileOnly(libs.spotbugsAnnotations)

    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)
    testImplementation(libs.assertj)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testRuntimeOnly(libs.log4jCore)
}
