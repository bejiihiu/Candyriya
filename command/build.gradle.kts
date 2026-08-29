plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candyriya.quality")
}

spotbugs {
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
}

dependencies {
    api(project(":permissions"))
    implementation(project(":config"))
    implementation(project(":network"))
    implementation(libs.log4jApi)
    implementation(libs.guava)
    implementation(libs.adventureApi)
    implementation(libs.adventureMiniMessage)
    implementation(libs.adventureGson)
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
