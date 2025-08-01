import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "org.liamjd.cantilever.openapi"
version = "0.0.11"

repositories {
    mavenCentral()
}

dependencies {
    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Replace 17 with your desired JDK version
    }
}