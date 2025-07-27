plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.liamjd.cantilever.openapi"
version = "0.1.1"

repositories {
    mavenCentral()
}

dependencies {
    // serialization
    implementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
