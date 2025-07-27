plugins {
    alias(libs.plugins.kotlin.jvm)
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
}

dependencies {
    // serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    // openAPI dependency scanning
    implementation(project(":OpenAPISchemaAnnotations"))
//    ksp(project(":OpenAPISchemaGenerator"))
//    ksp("org.liamjd.apiviaduct:openapi:0.4-SNAPSHOT")

    // multiplatform datetime library
    implementation(libs.kotlinx.datetime)
    
    // coroutines
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.jdk)

    // sdk v2
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)
    implementation(libs.aws.sdk.lambda)
    implementation(libs.aws.sdk.sqs)
    implementation(libs.aws.sdk.dynamodb)

    // testing
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.mockk)

    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.testcontainers:junit-jupiter:1.18.3")
    testImplementation("org.testcontainers:localstack:1.20.0") // For AWS services

    // routing
    implementation(libs.viaduct.openapi)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
