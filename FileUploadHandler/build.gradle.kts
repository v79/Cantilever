import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.liamjd.cantilever.lambda"
version = "0.1.3"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

dependencies {
    // shared elements
    implementation(project(":SharedModels"))
    implementation(libs.kotlinx.datetime)

    // serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    // coroutines
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.jdk)

    // sdk v2
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)
    implementation(libs.aws.sdk.lambda)
    implementation(libs.aws.sdk.sqs)
    implementation(libs.aws.sdk.dynamodb)

    // lambda functions
    implementation(libs.aws.lambda.core)
    implementation(libs.aws.lambda.events)
    runtimeOnly(libs.aws.lambda.log4j2)

    // DI
    implementation(libs.koin.core)

    // testing
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.aws.lambda.tests)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.koin.test)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<ShadowJar> {
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveBaseName.set("FileUploadHandler")
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
}
