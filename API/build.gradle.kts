import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.1.21"
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // routing
    implementation("org.liamjd.apiviaduct:router:0.4-SNAPSHOT")
    implementation("org.liamjd.apiviaduct:openapi:0.4-SNAPSHOT")

    // openAPI dependency scanning
    implementation(project(":OpenAPISchemaAnnotations"))
    implementation(project(":OpenAPISchemaGenerator"))

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.charleskorn.kaml:kaml:0.55.0")

    // DI
    implementation("io.insert-koin:koin-core:3.4.0")

    // sdk v2
    implementation(platform("software.amazon.awssdk:bom:2.20.68"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:sqs")

    // lambda functions
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")

    // auth
    implementation("com.auth0:java-jwt:4.3.0")
    implementation("com.auth0:jwks-rsa:0.22.0")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("com.amazonaws:aws-lambda-java-tests:1.1.1")
    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("io.insert-koin:koin-test:3.3.3")
    testImplementation("io.insert-koin:koin-test-junit5:3.3.3")
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
