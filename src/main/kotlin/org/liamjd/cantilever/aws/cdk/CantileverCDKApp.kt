package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps

fun main() {
    println("Initiating CDK Application")
    val app = App()

    val stack = CantileverStack(
        app,
        "CantileverStack",
        StackProps.builder().description("Cantilever is cloud-native static site generator").build()
    )

    app.synth()

}

fun makeEnv(account: String, region: String): Environment {
    return Environment.builder()
        .account(account)
        .region(region)
        .build()
}