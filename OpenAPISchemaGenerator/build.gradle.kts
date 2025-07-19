plugins {
    kotlin("jvm") version "2.2.0"
}

group = "org.liamjd.cantilever.openapi"
version = "0.1.0"

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
    implementation("com.google.devtools.ksp:symbol-processing-api:2.2.0-2.0.2")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
