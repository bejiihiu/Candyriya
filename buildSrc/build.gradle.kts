plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlinGradlePlugin)
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:${libs.versions.spotbugsGradle.get()}")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:${libs.versions.ktlintGradle.get()}")
}
