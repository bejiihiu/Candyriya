plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("candiriya.quality")
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":config"))
    implementation(project(":network"))
    implementation(project(":protocol"))
    implementation(libs.log4jApi)
    runtimeOnly(libs.log4jCore)
    runtimeOnly(libs.disruptor)
    implementation(libs.guava)
    compileOnly(libs.checkerQual)
}

application {
    mainClass.set("kz.bejiihiu.candiriya.launcher.MainKt")
}

tasks.named("shadowJar") {
    (this as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar).apply {
        archiveBaseName.set("candiriya")
        archiveClassifier.set("")
        archiveVersion.set("")
        mergeServiceFiles()
    }
}

tasks.named<JavaExec>("run") {
    // let gradle run use the shadow classpath? default is fine
    standardInput = System.`in`
}
