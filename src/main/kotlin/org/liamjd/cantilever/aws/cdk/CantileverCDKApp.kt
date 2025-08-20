package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps

fun main() {
    println("Initiating CDK Application")
    val app = App()
    val versionString = "v0.1.2"

    val euWest = makeEnv(System.getenv("CDK_DEFAULT_ACCOUNT"), "eu-west-2")

    println("Creating Cantilever stacks for $versionString in ${euWest.region} (${euWest.account})")
    val prodStack = CantileverStack(
        scope = app,
        id = "CantileverStack",
        props = buildStack("prod", euWest),
        versionString = versionString,
        deploymentDomain = "https://app.cantilevers.org",
        apiDomain = "api.cantilevers.org",
        isProd = true
    )

    val devStack = CantileverStack(
        scope = app,
        id = "Cantilever-Dev-Stack",
        props = buildStack("dev", euWest),
        versionString = "${versionString}-SNAPSHOT",
        deploymentDomain = "http://localhost:5173",
        apiDomain = "dev-api.cantilevers.org",
        isProd = false
    )

    app.synth()

}

fun makeEnv(account: String, region: String): Environment {
    return Environment.builder()
        .account(account)
        .region(region)
        .build()
}

fun buildStack(stageName: String, env: Environment): StackProps {
    return StackProps.builder().description("Cantilever ${stageName.uppercase()} is cloud-native static site generator")
        .stackName("cantilever-$stageName")
        .env(env).build()
}
