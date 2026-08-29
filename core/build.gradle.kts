plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candyriya.quality")
}

dependencies {
    api(project(":config"))
    api(project(":scheduler"))
    api(project(":network"))
    api(project(":permissions"))
    api(project(":command"))
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
