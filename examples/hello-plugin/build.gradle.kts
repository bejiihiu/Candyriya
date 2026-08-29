plugins {
    kotlin("jvm") version "2.4.0"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":plugin-api"))
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
    compileOnly("net.kyori:adventure-api:4.24.0")
}

// build a plain jar with plugin.json at root (no shadow needed for example)
tasks.jar {
    archiveBaseName.set("hello-plugin")
    from("src/main/resources") {
        include("plugin.json")
    }
}
