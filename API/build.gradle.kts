import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.liamjd.cantilever"
version = "0.1.0"

repositories {
    mavenCentral()
    google()
    mavenLocal()
    maven(url = "https://jitpack.io")
}


dependencies {
    // shared elements
    implementation(project(":SharedModels"))
    implementation(libs.kotlinx.datetime)

    // routing
    implementation(libs.viaduct.router)
    implementation(libs.viaduct.openapi)

    // openAPI dependency scanning
    implementation(project(":OpenAPISchemaAnnotations"))
    implementation(project(":OpenAPISchemaGenerator"))

    // serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    // DI
    implementation(libs.koin.core)

    // sdk v2
    implementation(platform(libs.aws.sdk.bom))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:sqs")

    // lambda functions
    implementation(libs.bundles.aws.lambda)

    // auth
    implementation(libs.java.jwt)
    implementation(libs.jwks.rsa)

    // testing
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.aws.lambda.tests)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.koin.test)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    dependsOn(
        parent?.project?.tasks?.named("copyAPISchema")
    )
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.getByName<Jar>("jar") {
    dependsOn(
        parent?.project?.tasks?.named("copyAPISchema")
    )
}

tasks.withType<ShadowJar> {
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveBaseName.set("APIRouter")
    dependsOn(
        parent?.project?.tasks?.named("copyAPISchema")
    )
}
