import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

group = "org.liamjd.cantilever.lambda"
version = "0.0.10"

repositories {
    mavenCentral()
    google()
}

dependencies {
    // shared elements
    implementation(project(":SharedModels"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.charleskorn.kaml:kaml:0.55.0")

    // sdk v2
    implementation(platform("software.amazon.awssdk:bom:2.20.68"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:sqs")

    // lambda functions
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")
    runtimeOnly("com.amazonaws:aws-lambda-java-log4j2:1.6.0")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("com.amazonaws:aws-lambda-java-tests:1.1.1")
    testImplementation("io.mockk:mockk:1.13.4")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<ShadowJar> {
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveBaseName.set("ImageProcessorHandler")
}
