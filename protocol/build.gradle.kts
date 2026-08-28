plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candiriya.quality")
}

dependencies {
    api(libs.nettyBuffer)
    api(libs.nettyCodec)
    api(libs.nettyHandler)
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
    testImplementation(libs.nettyTransport)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testRuntimeOnly(libs.log4jCore)
}

tasks.withType<Test>().configureEach {
    jvmArgs("-Dio.netty.leakDetectionLevel=paranoid")
}
