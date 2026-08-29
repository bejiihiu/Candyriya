plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candyriya.quality")
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":config"))
    implementation(project(":network"))
    implementation(project(":protocol"))
    implementation(project(":permissions"))
    implementation(project(":command"))
    implementation(libs.adventureApi)
    implementation(libs.adventureMiniMessage)
    implementation(libs.log4jApi)
    runtimeOnly(libs.log4jCore)
    runtimeOnly(libs.disruptor)
    implementation(libs.guava)
    compileOnly(libs.checkerQual)
    compileOnly(libs.spotbugsAnnotations)
}

application {
    mainClass.set("kz.bejiihiu.candyriya.launcher.MainKt")
}

tasks.named("shadowJar") {
    (this as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar).apply {
        archiveBaseName.set("candyriya")
        archiveClassifier.set("")
        archiveVersion.set("")
        mergeServiceFiles()
    }
}

tasks.named<JavaExec>("run") {
    // let gradle run use the shadow classpath? default is fine
    standardInput = System.`in`
}
