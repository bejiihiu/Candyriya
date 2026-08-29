plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candiriya.quality")
}

spotbugs {
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
    ignoreFailures.set(true)
}

dependencies {
    api(project(":plugin-api"))
    api(project(":scheduler"))
    api(project(":config"))
    implementation(project(":command"))
    implementation(project(":permissions"))
    implementation(project(":network"))
    implementation(libs.log4jApi)
    implementation(libs.guava)
    implementation(libs.adventureApi)
    implementation(libs.adventureMiniMessage)
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
