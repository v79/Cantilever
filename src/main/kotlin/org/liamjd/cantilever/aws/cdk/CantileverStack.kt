package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.*
import software.amazon.awscdk.services.apigateway.CorsOptions
import software.amazon.awscdk.services.apigateway.DomainNameOptions
import software.amazon.awscdk.services.apigateway.EndpointType
import software.amazon.awscdk.services.apigateway.LambdaRestApi
import software.amazon.awscdk.services.certificatemanager.Certificate
import software.amazon.awscdk.services.cognito.*
import software.amazon.awscdk.services.events.targets.SqsQueue
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.EventType
import software.amazon.awscdk.services.s3.NotificationKeyFilter
import software.amazon.awscdk.services.s3.deployment.BucketDeployment
import software.amazon.awscdk.services.s3.deployment.Source
import software.amazon.awscdk.services.sqs.Queue
import software.constructs.Construct

class CantileverStack(scope: Construct, id: String, props: StackProps?) : Stack(scope, id, props) {

    enum class ENV {
        destination_bucket,
        source_bucket,
        markdown_processing_queue,
        handlebar_template_queue,
        cors_domain
    }
    // TODO: I suppose I'm going to need to set up a dev and production environment for this sort of thing. Boo.

    constructor(scope: Construct, id: String) : this(scope, id, null)

    init {
        // Get the "deploymentDomain" value from cdk.json, or default to the dev URL if not found
        @Suppress("UNCHECKED_CAST")
        val envKey =  scope.node.tryGetContext("env") as String?
        val env = scope.node.tryGetContext(envKey ?: "env") as LinkedHashMap<String, String>?
        val deploymentDomain = (env?.get("domainName")) ?: "http://localhost:5173"
        println("ENVIRONMENT: $env; deploymentDomain: $deploymentDomain")

        Tags.of(this).add("Cantilever", "v0.0.4")

        // Source bucket where Markdown, template files will be stored
        // I may wish to change the removal and deletion policies
        println("Creating source bucket")
        val sourceBucket = createBucket("cantilever-sources")

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
                Queue.Builder.create(this, "cantilever-html-handlebar-queue").visibilityTimeout(Duration.minutes(3))
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
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.markdown_processing_queue.name to markdownProcessingQueue.queue.queueUrl
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
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.handlebar_template_queue.name to handlebarProcessingQueue.queue.queueUrl
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
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.destination_bucket.name to destinationBucket.bucketName,
            )
        )

        println("Creating API routing Lambda function")
        val apiRoutingLambda = createLambda(
            stack = this,
            id = "cantilever-api-router-lambda",
            description = "Lambda function which handles API routing, for API Gateway",
            codePath = "./API/build/libs/APIRouter.jar",
            handler = "org.liamjd.cantilever.api.LambdaRouter",
            environment = mapOf(
                ENV.source_bucket.name to sourceBucket.bucketName,
                ENV.destination_bucket.name to destinationBucket.bucketName,
                ENV.cors_domain.name to deploymentDomain
            )
        )

        println("Setting up website domain and cloudfront distribution for destination website bucket (not achieving its goal right now)")
        val cloudfrontSubstack = CloudFrontSubstack()
        val cloudFrontDistribution =
            cloudfrontSubstack.createCloudfrontDistribution(this, sourceBucket, destinationBucket)

        // I suspect this isn't the most secure way to do this. Better a new IAM role?
        println("Granting lambda permissions to buckets")
        fileUploadLambda.apply {
            sourceBucket.grantRead(this)
            sourceBucket.grantWrite(this)
        }
        markdownProcessorLambda.apply {
            sourceBucket.grantRead(this)
            sourceBucket.grantWrite(this)
        }
        templateProcessorLambda.apply {
            sourceBucket.grantRead(this)
            destinationBucket.grantWrite(this)
        }
        apiRoutingLambda.apply {
            sourceBucket.grantRead(this)
            sourceBucket.grantWrite(this)
        }

        println("Add S3 PUT/PUSH event source to fileUpload lambda")
        fileUploadLambda.addEventSource(
            S3EventSource.Builder.create(sourceBucket)
                .filters(listOf(NotificationKeyFilter.Builder().prefix("sources/").build()))
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

        println("Creating API Gateway integrations")
        val certificate = Certificate.fromCertificateArn(
            this,
            "cantilever-api-edge-certificate",
            "arn:aws:acm:us-east-1:086949310404:certificate/9b8f27c6-87be-4c14-a368-e6ad3ac4fb68"
        )
        val gateway = LambdaRestApi.Builder.create(this, "cantilever-rest-api")
            .restApiName("Cantilever REST API")
            .description("Gateway function to Cantilever services, handling routing")
            .domainName(
                DomainNameOptions.Builder().endpointType(EndpointType.EDGE).domainName("api.cantilevers.org")
                    .certificate(certificate).build()
            )
            .defaultCorsPreflightOptions(
                CorsOptions.builder()
                    .allowHeaders(listOf("'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"))
                    .allowMethods(listOf("GET", "OPTIONS","DELETE")).allowOrigins(listOf(deploymentDomain)).build()
            )
            .handler(apiRoutingLambda)
            .proxy(true)
            .build()

        println("Creating Cognito identity pool")
        val pool = UserPool.Builder.create(this, "cantilever-user-pool").userPoolName("cantilever-user-pool")
            .signInCaseSensitive(true)
            .signInAliases(SignInAliases.builder().email(true).phone(false).username(false).build())
            .passwordPolicy(PasswordPolicy.builder().minLength(12).build())
            .mfa(Mfa.OFF) // TODO: change this later
            .accountRecovery(AccountRecovery.EMAIL_ONLY)
            .selfSignUpEnabled(false)
            .email(UserPoolEmail.withCognito())
            .build()

        val cognitoPoolDomain = pool.addDomain(
            "cantilever-api",
            UserPoolDomainOptions.builder()
                .cognitoDomain(CognitoDomainOptions.builder().domainPrefix("cantilever").build()).build()
        )
        val appUrls = listOf("https://www.cantilevers.org/app/", "http://localhost:5173/")
        val appClient = pool.addClient(
            "cantilever-app",
            UserPoolClientOptions.builder().authFlows(AuthFlow.builder().build()).oAuth(
                OAuthSettings.builder().flows(OAuthFlows.builder().implicitCodeGrant(true).build())
                    .callbackUrls(appUrls).logoutUrls(appUrls).build()
            ).build()
        )
        /* println("Adding Cognito authentication to API Gateway")
          val authorizer = CognitoUserPoolsAuthorizer.Builder.create(this,"CantileverCognitoAuth").authorizerName("CantileverCognitoAuth").cognitoUserPools(
              listOf(pool)).build()*/


    }

    private fun createDestinationBucket(): Bucket = Bucket.Builder.create(this, "cantilever-website")
        .versioned(false)
        .removalPolicy(RemovalPolicy.DESTROY)
        .autoDeleteObjects(true)
        .publicReadAccess(true)
        .websiteIndexDocument("index.html")
        .build()

    private fun createBucket(name: String): Bucket = Bucket.Builder.create(this, name)
        .versioned(false)
        .removalPolicy(RemovalPolicy.DESTROY)
        .autoDeleteObjects(true)
        .build()

    /**
     * Create a lambda function with several assumptions:
     * - Java 11 runtime
     * - 320Mb RAM
     * - 2 minute timeout
     * - one month's logs
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
        .logRetention(RetentionDays.ONE_MONTH)
        .environment(environment ?: emptyMap())  // TODO should this should be a CloudFormation parameter CfnParameter
        .build()
}