import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.6.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.liamjd.cantilever"
version = "0.0.4"

repositories {
    mavenCentral()
    google()
    mavenLocal()
    maven(url ="https://jitpack.io" )
}

dependencies {
    // shared elements
    implementation(project(":SharedModels"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
    implementation("com.charleskorn.kaml:kaml:0.49.0")

     // DI
    implementation("io.insert-koin:koin-core:3.3.2")

    // sdk v2
    implementation(platform("software.amazon.awssdk:bom:2.19.8"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:sqs")

    // lambda functions
    implementation("com.amazonaws:aws-lambda-java-core:1.2.2")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.0")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("com.amazonaws:aws-lambda-java-tests:1.1.1")
    testImplementation("io.mockk:mockk:1.13.3")
    testImplementation("io.insert-koin:koin-test:3.3.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

tasks.withType<ShadowJar> {
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveBaseName.set("APIRouter")
}