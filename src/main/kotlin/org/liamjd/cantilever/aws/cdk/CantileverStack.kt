package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.*
import software.amazon.awscdk.services.apigateway.CorsOptions
import software.amazon.awscdk.services.apigateway.DomainNameOptions
import software.amazon.awscdk.services.apigateway.EndpointType
import software.amazon.awscdk.services.apigateway.LambdaRestApi
import software.amazon.awscdk.services.certificatemanager.Certificate
import software.amazon.awscdk.services.cloudfront.BehaviorOptions
import software.amazon.awscdk.services.cloudfront.CachePolicy
import software.amazon.awscdk.services.cloudfront.Distribution
import software.amazon.awscdk.services.cloudfront.DistributionProps
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin
import software.amazon.awscdk.services.cognito.*
import software.amazon.awscdk.services.dynamodb.*
import software.amazon.awscdk.services.events.targets.SqsQueue
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.LoggingFormat
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource
import software.amazon.awscdk.services.logs.LogGroup
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.route53.ARecord
import software.amazon.awscdk.services.route53.HostedZone
import software.amazon.awscdk.services.route53.HostedZoneAttributes
import software.amazon.awscdk.services.route53.RecordTarget
import software.amazon.awscdk.services.route53.targets.ApiGateway
import software.amazon.awscdk.services.s3.BlockPublicAccess
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.EventType
import software.amazon.awscdk.services.s3.ObjectOwnership
import software.amazon.awscdk.services.sqs.Queue
import software.constructs.Construct

/**
 * This class builds the Cantilever stack.
 * @param scope The AWS application scope.
 * @param id The ID of the stack
 * @param props Stack properties, can be null but should be set when calling
 * @param versionString The version of Cantilever being deployed
 * @param isProd Whether this is a production deployment or not
 */
class CantileverStack(
    scope: Construct,
    id: String,
    props: StackProps?,
    versionString: String,
    deploymentDomain: String,
    apiDomain: String,
    isProd: Boolean = false
) : Stack(scope, id, props) {

    enum class ENV {
        destination_bucket, source_bucket, generation_bucket, markdown_processing_queue, handlebar_template_queue, image_processing_queue, cors_domain, cognito_region, cognito_user_pools_id, dynamo_table_name
    }

    // TODO: I suppose I'm going to need to set up a dev and production environment for this sort of thing. Boo.
    var stageName: String

    constructor(scope: Construct, id: String) : this(
        scope, id, null, "vUnknown", "http://localhost:5173", "dev-api.cantilevers.org", false
    )

    // console colour codes
    val red = "\u001b[31m"
    val green = "\u001b[32m"
    val yellow = "\u001b[33m"
    val blue = "\u001b[34m"
    val magenta = "\u001b[35m"
    val cyan = "\u001b[36m"
    val white = "\u001b[37m"

    // Resets previous color codes
    val reset = "\u001b[0m"

    init {
        // Get the "deploymentDomain" value from cdk.json, or default to the dev URL if not found
        val envKey = scope.node.tryGetContext("env") as String?
        stageName = props?.stackName ?: "no-stage"

        @Suppress("UNCHECKED_CAST") val env =
            scope.node.tryGetContext(envKey ?: "env") as LinkedHashMap<String, String>?
        println()
        println(blue + "STACK $stackName" + reset)
        println(blue + "STAGE: ${stageName};  ENVIRONMENT: $env; deploymentDomain: $deploymentDomain; isPROD: $isProd" + reset)

        Tags.of(this).add("stageName", versionString)

        // Source bucket where Markdown, template files will be stored
        // I may wish to change the removal and deletion policies
        println("Creating source bucket")
        val sourceBucket = createBucket("sources")

        println("Creating intermediate generated bucket")
        val generationBucket = createBucket("generated")

        println("Creating destination website bucket")
        val destinationBucket = createDestinationBucket()

        println("Creating editor bucket")
        val editorBucket = createEditorBucket()

        var editorBucketDistribution: Distribution? = null
        if (!isProd) {
            println("Creating Cloudfront distribution for editor bucket in development mode")
            // Unfortunately, the distribution won't be invalidated automatically, so I need to do that manually
            // Or disable caching
            editorBucketDistribution = Distribution(
                this,
                "${stageName}-editor-bucket-distribution",
                DistributionProps.builder().comment("${stageName}-editor-bucket-distribution")
                    .defaultBehavior(
                        BehaviorOptions.builder().origin(S3BucketOrigin.withOriginAccessControl(editorBucket))
                            .cachePolicy(CachePolicy.CACHING_DISABLED).build()
                    ).defaultRootObject("index.html").enableLogging(true).build()
            )
        }

        // SQS for inter-lambda communication. The visibility timeout should be > the max processing time of the lambdas, so setting to 3
        println("Creating markdown processing queue")
        val markdownProcessingQueue = buildSQSQueue("cantilever-markdown-to-html-queue", 3)

        println("Creating handlebar templating processing queue")
        val handlebarProcessingQueue = buildSQSQueue("cantilever-html-handlebar-queue", 3)

        println("Creating image processing queue")
        val imageProcessingQueue = buildSQSQueue("cantilever-image-processing-queue", 3)

        println("Creating FileUploadHandler Lambda function")
        val fileUploadLambda = createLambda(
            stack = this,
            id = "${stageName}-file-upload",
            description = "Lambda function which responds to file upload events",
            codePath = "./FileUploadHandler/build/libs/FileUploadHandler.jar",
            handler = "org.liamjd.cantilever.lambda.FileUploadHandler",
            memory = 256,
            isProd = isProd,
            environment = mapOf(
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.markdown_processing_queue.name to markdownProcessingQueue.queue.queueUrl,
                ENV.handlebar_template_queue.name to handlebarProcessingQueue.queue.queueUrl,
                ENV.image_processing_queue.name to imageProcessingQueue.queue.queueUrl
            )
        )

        println("Creating MarkdownProcessorHandler Lambda function")
        val markdownProcessorLambda = createLambda(
            stack = this,
            id = "${stageName}-markdown-processor",
            description = "Lambda function which converts a markdown file to an HTML file or fragment",
            codePath = "./MarkdownProcessor/build/libs/MarkdownProcessorHandler.jar",
            handler = "org.liamjd.cantilever.lambda.md.MarkdownProcessorHandler",
            memory = 320,
            isProd = isProd,
            environment = mapOf(
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.generation_bucket.name to generationBucket.bucketName,
                ENV.handlebar_template_queue.name to handlebarProcessingQueue.queue.queueUrl,
                ENV.image_processing_queue.name to imageProcessingQueue.queue.queueUrl
            )
        )

        println("Creating TemplateProcessorHandler Lambda function")
        val templateProcessorLambda = createLambda(
            stack = this,
            id = "${stageName}-handlebar-processor",
            description = "Lambda function which renders a handlebars template with the given HTML fragment after markdown processing",
            codePath = "./TemplateProcessor/build/libs/TemplateProcessorHandler.jar",
            handler = "org.liamjd.cantilever.lambda.TemplateProcessorHandler",
            isProd = isProd,
            environment = mapOf(
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.generation_bucket.name to generationBucket.bucketName,
                ENV.destination_bucket.name to destinationBucket.bucketName,
            )
        )

        println("Creating Cognito identity pool")
        val cPool =
            UserPool.Builder.create(this, "cantilever-user-pool-$stageName").userPoolName("$stageName-user-pool")
                .signInCaseSensitive(true)
                .signInAliases(SignInAliases.builder().email(true).phone(false).username(false).build())
                .passwordPolicy(PasswordPolicy.builder().minLength(12).build()).mfa(Mfa.OFF) // TODO: change this later
                .accountRecovery(AccountRecovery.EMAIL_ONLY).selfSignUpEnabled(false).email(UserPoolEmail.withCognito())
                .removalPolicy(if (isProd) RemovalPolicy.RETAIN else RemovalPolicy.DESTROY).build()

        cPool.addDomain(
            "${stageName}-api-domain}",
            UserPoolDomainOptions.builder()
                .cognitoDomain(CognitoDomainOptions.builder().domainPrefix(stageName).build()).build()
        )

        val cr = props?.env?.region ?: "eu-west-2" // Default to eu-west-2 if not set
        println("Creating API routing Lambda function for Cognito region '$cr'")
        val corsDomain = if (isProd) {
            deploymentDomain
        } else {
            "https://dco7fhfjo6vkm.cloudfront.net" // hard-coded for now
        }
        val apiRoutingLambda = createLambda(
            stack = this,
            id = "${stageName}-api-router",
            description = "Lambda function which handles API routing, for API Gateway",
            codePath = "./API/build/libs/APIRouter.jar",
            handler = "org.liamjd.cantilever.api.NewLambdaRouter",
            memory = 360,
            isProd = isProd,
            environment = mapOf(
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.generation_bucket.name to generationBucket.bucketName,
                ENV.markdown_processing_queue.name to markdownProcessingQueue.queue.queueUrl,
                ENV.handlebar_template_queue.name to handlebarProcessingQueue.queue.queueUrl,
                ENV.image_processing_queue.name to imageProcessingQueue.queue.queueUrl,
                ENV.cors_domain.name to corsDomain,
                ENV.cognito_region.name to cr,
                ENV.cognito_user_pools_id.name to cPool.userPoolId
            )
        )

        println("Creating image processing Lambda function")
        val imageProcessorLambda = createLambda(
            stack = this,
            id = "${stageName}-image-processor-lambda",
            description = "Lambda function which processes images",
            codePath = "./ImageProcessor/build/libs/ImageProcessorHandler.jar",
            handler = "org.liamjd.cantilever.lambda.image.ImageProcessorHandler",
            memory = 256,
            isProd = isProd,
            environment = mapOf(
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.generation_bucket.name to generationBucket.bucketName,
                ENV.destination_bucket.name to destinationBucket.bucketName,
            )
        )

        /*println("Setting up website domain and cloudfront distribution for destination website bucket (not achieving its goal right now)")
        val cloudfrontSubstack =
            CloudFrontSubstack(
                versionString,
                if (props != null) props.stackName ?: "Cantilever-null-Stack" else "CantileverStack"
            )
        cloudfrontSubstack.createCloudfrontDistribution(this, destinationBucket)*/

        // I suspect this isn't the most secure way to do this. Better a new IAM role?
        println("Granting lambda permissions to buckets and queues")
        fileUploadLambda.apply {
            sourceBucket.grantRead(this)
        }
        markdownProcessorLambda.apply {
            sourceBucket.grantRead(this)
            generationBucket.grantWrite(this)
        }
        templateProcessorLambda.apply {
            sourceBucket.grantRead(this)
            generationBucket.grantRead(this)
            destinationBucket.grantWrite(this)
        }
        apiRoutingLambda.apply {
            sourceBucket.grantRead(this)
            sourceBucket.grantWrite(this)
            generationBucket.grantRead(this)
            generationBucket.grantWrite(this)
        }
        imageProcessorLambda.apply {
            sourceBucket.grantRead(this)
            generationBucket.grantWrite(this)
            destinationBucket.grantWrite(this)
        }

        println("Add S3 PUT/PUSH event source to fileUpload lambda")
        fileUploadLambda.addEventSource(
            S3EventSource.Builder.create(sourceBucket)
                .events(mutableListOf(EventType.OBJECT_CREATED_PUT, EventType.OBJECT_CREATED_POST, EventType.OBJECT_REMOVED)).build()
        )

        println("Add markdown processor SQS event source to markdown processor lambda")
        markdownProcessorLambda.addEventSource(
            SqsEventSource.Builder.create(markdownProcessingQueue.queue).build()
        )

        println("Add template processor SQS event source to template processor lambda")
        templateProcessorLambda.addEventSource(
            SqsEventSource.Builder.create(handlebarProcessingQueue.queue).build()
        )

        println("Add image processor SQS event source to image processor lambda")
        imageProcessorLambda.addEventSource(
            SqsEventSource.Builder.create(imageProcessingQueue.queue).build()
        )

        println("Granting queue-to-queue permissions")
        fileUploadLambda.apply {
            markdownProcessingQueue.queue.grantSendMessages(this)
            handlebarProcessingQueue.queue.grantSendMessages(this)
            imageProcessingQueue.queue.grantSendMessages(this)
        }
        markdownProcessorLambda.apply {
            markdownProcessingQueue.queue.grantConsumeMessages(this)
            handlebarProcessingQueue.queue.grantSendMessages(this)
            imageProcessingQueue.queue.grantSendMessages(this)
        }
        templateProcessorLambda.apply {
            handlebarProcessingQueue.queue.grantConsumeMessages(this)
        }
        apiRoutingLambda.apply {
            markdownProcessingQueue.queue.grantSendMessages(this)
            handlebarProcessingQueue.queue.grantSendMessages(this)
        }
        imageProcessorLambda.apply {
            imageProcessingQueue.queue.grantConsumeMessages(this)
        }

        println("Creating API Gateway integrations")
        val certificate = Certificate.fromCertificateArn(
            this,
            "${stageName}-api-edge-certificate}",
            "arn:aws:acm:us-east-1:086949310404:certificate/9b8f27c6-87be-4c14-a368-e6ad3ac4fb68"
        )

        // The API Gateway
        // I don't like how much I have to hardcode the allowed headers here. I would like this to be configurable by the router.
        println("Creating API Gateway with Lambda integration for $stageName to domain $apiDomain")
        val lambdaRestAPI = LambdaRestApi.Builder.create(this, "${stageName}-rest-api")
            .restApiName("Cantilever $stageName REST API")
            .description("Gateway function to Cantilever services, handling routing").disableExecuteApiEndpoint(true)
            .domainName(
                DomainNameOptions.Builder().endpointType(EndpointType.EDGE).domainName(apiDomain)
                    .certificate(certificate).build()
            ).defaultCorsPreflightOptions(
                CorsOptions.builder().allowHeaders(
                    listOf(
                        "Content-Type",
                        "Content-Length",
                        "X-Amz-Date",
                        "Authorization",
                        "X-Api-Key",
                        "X-Amz-Security-Token",
                        "X-Content-Length",
                        "Cantilever-Project-Domain",
                    )
                ).allowMethods(listOf("GET", "PUT", "POST", "OPTIONS", "DELETE")).allowOrigins(listOf(deploymentDomain, "https://www.cantilevers.org", "https://dco7fhfjo6vkm.cloudfront.net"))
                    .build()
            ).handler(apiRoutingLambda).proxy(true).build()


        /*      println("Create DynamoDB database tables: project")
              val projectTable = TableV2.Builder.create(this, "${stageName.uppercase()}-database-project-table")
                  .tableName("${stageName}-projects")
                  .removalPolicy(if (isProd) RemovalPolicy.RETAIN else RemovalPolicy.DESTROY).partitionKey(
                      Attribute.builder().name("domain").type(AttributeType.STRING).build()
                  ).sortKey(Attribute.builder().name("lastUpdated").type(AttributeType.NUMBER).build())
                  .globalSecondaryIndexes(
                      listOf(
                          GlobalSecondaryIndexPropsV2.builder().indexName("ALL_PROJECTS")
                              .partitionKey(Attribute.builder().name("domain").type(AttributeType.STRING).build()).sortKey(
                              Attribute.builder().name("lastUpdated").type(
                                  AttributeType.NUMBER
                              ).build()
                          ).projectionType(ProjectionType.ALL).build()
                      )
                  )
                  .build()
      */
        println("Creating DynamoDB database tables - PK domain#type, SK srcKey")
        val contentNodeTable = TableV2.Builder.create(this, "${stageName}-database-content-node-table")
            .tableName("${stageName}-content-nodes")
            .removalPolicy(if (isProd) RemovalPolicy.RETAIN else RemovalPolicy.DESTROY).partitionKey(
                Attribute.builder().name("domain#type").type(AttributeType.STRING).build()
            ).sortKey(Attribute.builder().name("srcKey").type(AttributeType.STRING).build())
            .globalSecondaryIndexes(
                listOf(
                    GlobalSecondaryIndexPropsV2.builder().indexName("Project-NodeType-LastUpdated")
                        .partitionKey(Attribute.builder().name("domain").type(AttributeType.STRING).build()).sortKey(
                            Attribute.builder().name("type#lastUpdated").type(
                                AttributeType.STRING
                            ).build()
                        ).projectionType(ProjectionType.ALL).build()
                )
            )
            .build()

        println("Granting permissions to the lambdas to access the DynamoDB table")
        contentNodeTable.grantReadWriteData(apiRoutingLambda)
        contentNodeTable.grantReadWriteData(fileUploadLambda)


        println("Creating API Gateway DNS record for $apiDomain")
        val apiDomainDNSRecord = ARecord.Builder.create(this, "${stageName}-api-record}").zone(
            HostedZone.fromHostedZoneAttributes(
                this,
                "${stageName}-api-zone}",
                HostedZoneAttributes.builder().hostedZoneId("Z01474271BEFJPTG86EOG").zoneName("cantilevers.org").build()
            )
        ).recordName(apiDomain).target(RecordTarget.fromAlias(ApiGateway(lambdaRestAPI))).build()


        // I want to put the domain name of the editor cloudfront distribution here, but it isn't available until after the stack is deployed
        // Gemini suggested writing a custom resource and a lambda function to execute post-deployment
        // The alternative may be have two separate stacks, one for the distribution and one for the rest of the resources
        // or I just hardcode the URL for the editor bucket distribution
        val appUrls = listOf(deploymentDomain, "https://dco7fhfjo6vkm.cloudfront.net")
        val corbelAppUrls = listOf(
            "http://localhost:44817/callback", "corbelApp://auth"
        ) // port randomly chosen here, needs to match that in the Corbel application
        println("Registering app clients with Cognito identity pool for domains $appUrls")
        cPool.addClient(
            "cantilever-app",
            UserPoolClientOptions.builder().userPoolClientName("${stageName}-webapp-client-pool")
                .authFlows(AuthFlow.builder().build()).oAuth(
                    OAuthSettings.builder().flows(OAuthFlows.builder().implicitCodeGrant(true).build())
                        .callbackUrls(appUrls).logoutUrls(appUrls).build()
                ).build()
        )
        cPool.addClient(
            "corbel-app",
            UserPoolClientOptions.builder().userPoolClientName("${stageName}-corbel-app-client-pool")
                .authFlows(AuthFlow.builder().build()).oAuth(
                    OAuthSettings.builder().flows(
                        OAuthFlows.builder().implicitCodeGrant(false).authorizationCodeGrant(true).build()
                    ).scopes(listOf(OAuthScope.EMAIL, OAuthScope.OPENID, OAuthScope.COGNITO_ADMIN))
                        .callbackUrls(corbelAppUrls).logoutUrls(corbelAppUrls).build()
                ).build()
        )

    }

    /**
     * Build an SQS queue with a visibility timeout.
     * @param queueId The ID of the queue
     * @param timeout The visibility timeout in minutes
     */
    private fun buildSQSQueue(queueId: String, timeout: Int): SqsQueue = SqsQueue.Builder.create(
        Queue.Builder.create(this, queueId).visibilityTimeout(
            Duration.minutes(timeout)
        ).build()
    ).build()

    /**
     * Create a destination bucket for the website content
     * This bucket has a fixed name, and access is cntrolled by the CloudFront distribution.
     */
    private fun createDestinationBucket(): Bucket =
        Bucket.Builder.create(this, "${stageName}-website}").versioned(false)
            .removalPolicy(RemovalPolicy.DESTROY).autoDeleteObjects(true).blockPublicAccess(
                BlockPublicAccess.BLOCK_ALL
            ).build()

    /**
     * Create a bucket with the given name.
     * @param name The name of the bucket, must be globally unique
     * @param public Whether the bucket should be publicly readable
     */
    private fun createBucket(name: String, public: Boolean = false): Bucket =
        Bucket.Builder.create(this, "${stageName}-${name}").versioned(false)
            .removalPolicy(RemovalPolicy.DESTROY).autoDeleteObjects(true).publicReadAccess(public).versioned(true)
            .build()

    /**
     * Create a bucket for the editor to store its static files.
     */
    private fun createEditorBucket(): Bucket =
        Bucket.Builder.create(this, "${stageName}-editor").versioned(false)
            .removalPolicy(RemovalPolicy.DESTROY).autoDeleteObjects(true).objectOwnership(ObjectOwnership.OBJECT_WRITER)
            .blockPublicAccess(BlockPublicAccess.BLOCK_ALL).websiteIndexDocument("index.html").build()

    /**
     * Create a lambda function with several assumptions:
     * - Java 21 runtime
     * - 320Mb RAM
     * - 2 minute timeout
     * - JSON logging format
     * - one month's logs
     */
    private fun createLambda(
        stack: Stack,
        id: String,
        description: String?,
        codePath: String,
        handler: String,
        memory: Int = 320,
        environment: Map<String, String>?,
        isProd: Boolean
    ): Function =
        Function.Builder.create(stack, id).description(description ?: "").runtime(Runtime.JAVA_21).memorySize(memory)
            .timeout(Duration.minutes(2)).code(Code.fromAsset(codePath)).handler(handler)
            .loggingFormat(LoggingFormat.JSON).logGroup(
                LogGroup.Builder.create(this, "/aws/lambda/${stack.stackName}/$id")
                    .logGroupName("/aws/lambda/${stack.stackName}/${id}").removalPolicy(RemovalPolicy.DESTROY)
                    .retention(
                        if (isProd) {
                            RetentionDays.ONE_MONTH
                        } else {
                            RetentionDays.ONE_WEEK
                        }
                    ).build()
            )
            .environment(
                environment ?: emptyMap()
            )  // TODO: should this should be a CloudFormation parameter CfnParameter
            .build()

    fun info(message: String) =
        println("\t" + green + message + reset)
}