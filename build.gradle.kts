import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    application
    `maven-publish`
    id("org.sonarqube") version "3.5.0.2730"
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
}

group = "org.liamjd"
version = "0.0.5"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

dependencies {
    // AWS CDK
    implementation("software.amazon.awscdk:aws-cdk-lib:2.58.1")
    implementation("software.constructs:constructs:10.1.222")

    // multiplatform datetime library
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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

koverMerged {
    enable()
}