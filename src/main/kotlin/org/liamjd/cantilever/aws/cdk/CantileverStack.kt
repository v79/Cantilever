package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.*
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.EventType
import software.amazon.awscdk.services.s3.deployment.BucketDeployment
import software.amazon.awscdk.services.s3.deployment.Source
import software.constructs.Construct

class CantileverStack(scope: Construct, id: String, props: StackProps?) : Stack(scope, id, props) {

    constructor(scope: Construct, id: String) : this(scope, id, null)

    init {
        Tags.of(this).add("Cantilever", "v0.1")

        // Source bucket where Markdown, template files will be stored
        // I may wish to change the removal and deletion policies
        println("Creating source bucket")
        val sourceBucket = createSourceBucket()


        println("Creating destination bucket")
        val destinationBucket = createDestinationBucket()

        println("Adding temporary index.html")
        val indexHtml = BucketDeployment.Builder.create(this, "cantilever-website-index")
            .sources(listOf(Source.asset("src/main/resources/staticBucket")))
            .destinationBucket(destinationBucket)
            .build()

        println("Creating FileUploadHandler Lambda function")
        val fileUploadLambda = createLambda(
            stack = this,
            id = "cantilever-file-upload-lambda",
            description = "Lambda function which responds to file upload events",
            codePath = "./FileUploadHandler/build/libs/fileUploadHandler.jar",
            handler = "org.liamjd.cantilever.lambda.FileUploadHandler",
            environment = mapOf("destination_bucket" to destinationBucket.bucketName)
        )

        // I suspect this isn't the most secure way to do this. Better a new IAM role?
        sourceBucket.grantRead(fileUploadLambda)
        destinationBucket.grantRead(fileUploadLambda)
        destinationBucket.grantWrite(fileUploadLambda)

        fileUploadLambda.addEventSource(
            S3EventSource.Builder.create(sourceBucket)
                .events(mutableListOf(EventType.OBJECT_CREATED_PUT, EventType.OBJECT_CREATED_POST)).build()
        )
    }

    private fun createDestinationBucket(): Bucket = Bucket.Builder.create(this, "cantilever-website")
        .versioned(false)
        .removalPolicy(RemovalPolicy.DESTROY)
        .autoDeleteObjects(true)
        .publicReadAccess(true)
        .websiteIndexDocument("index.html")
        .build()

    private fun createSourceBucket(): Bucket = Bucket.Builder.create(this, "cantilever-sources")
        .versioned(false)
        .removalPolicy(RemovalPolicy.DESTROY)
        .autoDeleteObjects(true)
        .build()

    /**
     * Create a lambda function with several assumptions:
     * - Java 11 runtime
     * - 256Mb RAM
     * - 2 minute timeout
     */
    private fun createLambda(
        stack: Stack,
        id: String,
        description: String?,
        codePath: String,
        handler: String,
        environment: Map<String, String>?
    ): Function = Function.Builder.create(stack, id)
        .description(description ?: "")
        .runtime(Runtime.JAVA_11)
        .memorySize(256)
        .timeout(Duration.minutes(2))
        .code(Code.fromAsset(codePath))
        .handler(handler)
        .environment(environment ?: emptyMap())  // TODO this should be a CloudFormation parameter CfnParameter
        .build()
}