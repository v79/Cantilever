package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps

fun main() {
    println("Initiating CDK Application")
    val app = App()
    val versionString = "v0.1.0"

    val euWest = makeEnv(System.getenv("CDK_DEFAULT_ACCOUNT"), "eu-west-2")

    println("Creating Cantilever stacks for $versionString in ${euWest.region} (${euWest.account})")
    val prodStack = CantileverStack(
        app,
        "CantileverStack",
        buildStack("prod", euWest),
        versionString
    )

    val devStack = CantileverStack(
        app,
        "Cantilever-Dev-Stack",
        buildStack("dev", euWest),
        "${versionString}-SNAPSHOT"
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
