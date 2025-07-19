package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps

fun main() {
    println("Initiating CDK Application")
    val app = App()
    val versionString = "v0.0.13"

    val euWest = makeEnv(System.getenv("CDK_DEFAULT_ACCOUNT"), "eu-west-2")

    val stack = CantileverStack(
        app,
        "CantileverStack",
        StackProps.builder().description("Cantilever is cloud-native static site generator").env(euWest).build(),
        versionString
    )

    app.synth()

}

fun makeEnv(account: String, region: String): Environment {
    return Environment.builder()
        .account(account)
        .region(region)
        .build()
}