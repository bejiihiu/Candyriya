import com.github.spotbugs.snom.SpotBugsTask

plugins {
    checkstyle
    id("com.github.spotbugs")
    id("org.jlleitschuh.gradle.ktlint")
}

checkstyle {
    toolVersion = "10.21.0"
    configFile = rootProject.file("config/checkstyle/google_checks.xml")
    isIgnoreFailures = false
    isShowViolations = true
}

spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    ignoreFailures.set(false)
}

tasks.withType<SpotBugsTask>().configureEach {
    reports {
        create("xml") { required.set(true) }
        create("html") { required.set(true) }
    }
    // yep kotlin generates false positives for coroutines xd
    if (name.contains("Test", ignoreCase = true)) {
        enabled = false
    }
}

// ktlint uses android style guide via .editorconfig
ktlint {
    android.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
    }
}

dependencies {
    // checker-qual for nullness annotations, provided as compileOnly
    add("compileOnly", "org.checkerframework:checker-qual:3.49.0")
}
