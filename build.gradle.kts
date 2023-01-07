import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
    `maven-publish`
}

group = "org.liamjd"
version = "0.0.1"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

dependencies {
    // AWS CDK
    implementation("software.amazon.awscdk:aws-cdk-lib:2.56.1")
    implementation("software.constructs:constructs:10.1.207")

    // multiplatform datetime library
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("org.liamjd.cantilever.aws.cdk.CantileverCDKAppKt")
}

tasks.withType<JavaExec> {
    dependsOn(":FileUploadHandler:shadowJar")
    dependsOn(":MarkdownProcessor:shadowJar")
}