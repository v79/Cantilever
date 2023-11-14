import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    application
    `maven-publish`
    id("org.sonarqube") version "4.4.1.3373"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

group = "org.liamjd"
version = "0.0.8"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

dependencies {
    // AWS CDK
    implementation("software.amazon.awscdk:aws-cdk-lib:2.104.0")
    implementation("software.constructs:constructs:10.3.0")

    // multiplatform datetime library
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("org.liamjd.cantilever.aws.cdk.CantileverCDKAppKt")
}

tasks.withType<JavaExec> {
    dependsOn(":FileUploadHandler:shadowJar")
    dependsOn(":MarkdownProcessor:shadowJar")
    dependsOn(":TemplateProcessor:shadowJar")
    dependsOn(":API:shadowJar")
}

tasks {
    getByName<Delete>("clean") {
        delete.add("cdk.out/asset*/") // add accepts argument with Any type
    }
}

koverReport {
    defaults {

    }
}