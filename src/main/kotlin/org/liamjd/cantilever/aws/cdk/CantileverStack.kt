package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.*
import software.amazon.awscdk.services.events.targets.SqsQueue
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.EventType
import software.amazon.awscdk.services.s3.deployment.BucketDeployment
import software.amazon.awscdk.services.s3.deployment.Source
import software.amazon.awscdk.services.sqs.Queue
import software.constructs.Construct

class CantileverStack(scope: Construct, id: String, props: StackProps?) : Stack(scope, id, props) {

    constructor(scope: Construct, id: String) : this(scope, id, null)

    init {
        Tags.of(this).add("Cantilever", "v0.0.2")

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

        // SQS for inter-lambda communication. The visibility timeout should be > the max processing time of the lambdas, so setting to 3
        println("Creating markdown processing queue")
        val markdownProcessingQueue =
            SqsQueue.Builder.create(
                Queue.Builder.create(this, "cantilever-markdown-to-html-queue").visibilityTimeout(
                    Duration.minutes(3)
                ).build()
            ).build()

        println("Creating handlebar templating processing queue")
        val handlebarProcessingQueue =
            SqsQueue.Builder.create(
                Queue.Builder.create(this, "cantiliver-html-handlebar-queue").visibilityTimeout(Duration.minutes(3))
                    .build()
            ).build()

        println("Creating FileUploadHandler Lambda function")
        val fileUploadLambda = createLambda(
            stack = this,
            id = "cantilever-file-upload-lambda",
            description = "Lambda function which responds to file upload events",
            codePath = "./FileUploadHandler/build/libs/FileUploadHandler.jar",
            handler = "org.liamjd.cantilever.lambda.FileUploadHandler",
            environment = mapOf(
                "destination_bucket" to destinationBucket.bucketName,
                "markdown_processing_queue" to markdownProcessingQueue.queue.queueUrl
            )
        )

        println("Creating MarkdownProcessorHandler Lambda function")
        val markdownProcessorLambda = createLambda(
            stack = this,
            id = "cantilever-markdown-processor-lambda",
            description = "Lambda function which converts a markdown file to an HTML file or fragment",
            codePath = "./MarkdownProcessor/build/libs/MarkdownProcessorHandler.jar",
            handler = "org.liamjd.cantilever.lambda.md.MarkdownProcessorHandler",
            environment = mapOf(
                "source_bucket" to sourceBucket.bucketName,
                "handlebar_template_queue" to handlebarProcessingQueue.queue.queueUrl
            )
        )

        println("Creating TemplateProcessorHandler Lambda function")
        val templateProcessorLambda = createLambda(
            stack = this,
            id = "cantilever-handlebar-processor-lambda",
            description = "Lambda function which renders a handlebars template with the given HTML fragment after markdown processing",
            codePath = "./TemplateProcessor/build/libs/TemplateProcessorHandler.jar",
            handler = "org.liamjd.cantilever.lambda.TemplateProcessorHandler",
            environment = mapOf(
                "source_bucket" to sourceBucket.bucketName,
                "destination_bucket" to destinationBucket.bucketName,
            )
        )

        // I suspect this isn't the most secure way to do this. Better a new IAM role?
        println("Granting lambda permissions")
        sourceBucket.grantRead(fileUploadLambda)
        sourceBucket.grantRead(markdownProcessorLambda)
        sourceBucket.grantWrite(markdownProcessorLambda)
        sourceBucket.grantRead(templateProcessorLambda)
        destinationBucket.grantWrite(templateProcessorLambda)

        println("Add S3 PUT/PUSH event source to fileUpload lambda")
        fileUploadLambda.addEventSource(
            S3EventSource.Builder.create(sourceBucket)
                .events(mutableListOf(EventType.OBJECT_CREATED_PUT, EventType.OBJECT_CREATED_POST)).build()
        )
        println("Add markdown processor SQS event source to markdown processor lambda")
        markdownProcessorLambda.addEventSource(
            SqsEventSource.Builder.create(markdownProcessingQueue.queue).build()
        )

        println("Add template processor SQS event source to template processor lambda")
        templateProcessorLambda.addEventSource(
            SqsEventSource.Builder.create(handlebarProcessingQueue.queue).build()
        )

        println("Granting queue permissions")
        markdownProcessingQueue.queue.grantSendMessages(fileUploadLambda)
        markdownProcessingQueue.queue.grantConsumeMessages(markdownProcessorLambda)
        handlebarProcessingQueue.queue.grantSendMessages(markdownProcessorLambda)
        handlebarProcessingQueue.queue.grantConsumeMessages(templateProcessorLambda)

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
     * - 320Mb RAM
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
        .memorySize(320)
        .timeout(Duration.minutes(2))
        .code(Code.fromAsset(codePath))
        .handler(handler)
        .environment(environment ?: emptyMap())  // TODO should this should be a CloudFormation parameter CfnParameter
        .build()
}