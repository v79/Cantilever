import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.liamjd.cantilever"
version = "0.1.3"

repositories {
    mavenCentral()
    google()
    mavenLocal()
    maven(url = "https://jitpack.io")
    maven {
        url = uri("https://maven.pkg.github.com/v79/APIViaduct")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
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
    
    // coroutines
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.jdk)

    // sdk v2
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.bundles.aws.sdk)

    // lambda functions
    implementation(libs.bundles.aws.lambda)

    // logging
    implementation(libs.bundles.logging)
    implementation(libs.aws.lambda.log4j2)

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
    dependsOn(rootProject.tasks.named("copyAPISchema"))
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.getByName<Jar>("jar") {
    dependsOn(rootProject.tasks.named("copyAPISchema"))
}

tasks.withType<ShadowJar> {
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveBaseName.set("APIRouter")
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
    dependsOn(rootProject.tasks.named("copyAPISchema"))
}
