plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.liamjd.cantilever"
version = "0.1.2"

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
//    ksp("org.liamjd.apiviaduct:openapi:0.4.1-SNAPSHOT")

    // multiplatform datetime library
    implementation(libs.kotlinx.datetime)
    
    // coroutines
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.jdk)

    // sdk v2
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.bundles.aws.sdk)
    implementation(libs.aws.lambda.core)

    // testing
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mockk)

    testImplementation(libs.bundles.testcontainers) // For AWS services

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
