plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candiriya.quality")
}

dependencies {
    implementation(libs.log4jApi)
    implementation(libs.guava)
    compileOnly(libs.checkerQual)

    testImplementation(libs.junitApi)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testRuntimeOnly(libs.log4jCore)
}
