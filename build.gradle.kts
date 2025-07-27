plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    `maven-publish`
    alias(libs.plugins.kover)
    id("org.barfuin.gradle.taskinfo") version "2.1.0"
}

group = "org.liamjd"
version = "0.1.1"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

dependencies {
    // AWS CDK
    implementation(libs.aws.cdk.lib)
    implementation(libs.aws.constructs)

    // multiplatform datetime library
    implementation(libs.kotlinx.datetime)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("org.liamjd.cantilever.aws.cdk.CantileverCDKAppKt")
}

tasks.withType<JavaExec> {
    dependsOn(":FileUploadHandler:shadowJar")
    dependsOn(":MarkdownProcessor:shadowJar")
    dependsOn(":TemplateProcessor:shadowJar")
    dependsOn(":ImageProcessor:shadowJar")
    dependsOn(":API:shadowJar")
}

tasks {
    getByName<Delete>("clean") {
        delete.add("cdk.out/asset*/") // add accepts argument with Any type
    }
}

tasks.register("copyAPISchema", Copy::class.java) {
    from(project(":SharedModels").layout.buildDirectory.dir("generated/ksp/main/resources/openapi/schema/api-schema.yaml"))
    into(project(":API").layout.buildDirectory.dir("/resources/main/schemas/"))
    doLast {
        println("Copied API schema to build/resources/main/schemas") }
}
