plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.7.20"
}

group = "org.liamjd.cantilever"
version = "0.0.4"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

dependencies {
    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("com.charleskorn.kaml:kaml:0.49.0")

    // multiplatform datetime library
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // sdk v2
    implementation(platform("software.amazon.awssdk:bom:2.19.8"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:sqs")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}