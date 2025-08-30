plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "org.liamjd.cantilever.openapi"
version = "0.1.3"

repositories {
    mavenCentral()
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.2.0"))
    }
}

dependencies {
    // shared elements
    implementation(project(":OpenAPISchemaAnnotations"))
    implementation(libs.kotlin.ksp)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation(libs.junit.jupiter)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
