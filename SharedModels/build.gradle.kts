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

    // sdk v2
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)
    implementation(libs.aws.sdk.lambda)
    implementation(libs.aws.sdk.sqs)

    // testing
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)

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
