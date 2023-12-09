import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
}

group = "org.liamjd.cantilever.openapi"
version = "0.0.9"

repositories {
    mavenCentral()
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.9.20"))
    }
}

dependencies {
    // shared elements
    implementation(project(":OpenAPISchemaAnnotations"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.20-1.0.14")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xcontext-receivers"
    }
}