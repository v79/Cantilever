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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

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
