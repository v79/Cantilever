plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

group = "org.liamjd.cantilever"
version = "0.0.12"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

dependencies {
    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.charleskorn.kaml:kaml:0.55.0")

    // openAPI dependency scanning
    implementation(project(":OpenAPISchemaAnnotations"))
//    ksp(project(":OpenAPISchemaGenerator"))
    ksp("org.liamjd.apiviaduct:openapi:0.4-SNAPSHOT")

    // multiplatform datetime library
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // sdk v2
    implementation(platform("software.amazon.awssdk:bom:2.20.68"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:dynamodb")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")


    implementation("org.liamjd.apiviaduct:openapi:0.4-SNAPSHOT")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Replace 17 with your desired JDK version
    }
}