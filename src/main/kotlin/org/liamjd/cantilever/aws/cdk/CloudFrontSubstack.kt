package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.Duration
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.Tags
import software.amazon.awscdk.services.cloudfront.*
import software.amazon.awscdk.services.cloudfront.experimental.EdgeFunction
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.BucketAccessControl
import software.amazon.awscdk.services.s3.IBucket

class CloudFrontSubstack(private val versionString: String) {

    // taken from https://johntipper.org/a-static-website-with-api-backend-using-aws-cdk-and-java/

    // Route53 hosted zone created out-of-band, e.g already existing
    // but this requires the environment to be configured in the stack, which means putting the account ID in git

    /*        val hostedZone = HostedZone.fromLookup(
                this, "HostedZone", HostedZoneProviderProps.builder()
                    .domainName("cantilevers.org")
                    .build()
            )*/

    private fun createLambdaEdge(stack: Stack): EdgeFunction {
        println("Create cloudfront Lambda@edge function")
        return EdgeFunction.Builder.create(stack, "cantilever-cloudfront-edgelambda-rewrite")
            .description("Cantilever cloudfront rewrite URI from host")
            .runtime(Runtime.NODEJS_LATEST)
            .memorySize(128)
            .timeout(Duration.seconds(5))
            .code(Code.fromAsset("./CloudfrontRewriteURI"))
            .handler("index.handler")
            .logRetention(RetentionDays.ONE_MONTH)
            .build()
    }

    /**
     * This isn't complete, there are extra steps I need to do at the AWS console:
     * - Attach the SSL certificate
     * - Add the alternate domain name (www...)
     */
    fun createCloudfrontDistribution(
        stack: Stack,
        destinationBucket: IBucket
    ): CloudFrontWebDistribution {

        Tags.of(stack).add("Cantilever", versionString)

        val lambdaEdgeFunction = createLambdaEdge(stack)
        // Origin Access Identity is considered legacy, to be replaced by Origin Access Control. But that's not
        // supported in CDK yet. #21771
        val webOai = OriginAccessIdentity.Builder.create(stack, "cantilever-originAccessIdentity").build()
        destinationBucket.grantRead(webOai)

        return CloudFrontWebDistribution.Builder.create(stack, "cantilever-CloudFrontWebDistribution")
            .comment(String.format("CloudFront distribution for cantilever"))
            .defaultRootObject("index.html")
            .originConfigs(
                listOf(
                    SourceConfiguration.builder()
                        .s3OriginSource(
                            S3OriginConfig.builder()
                                .s3BucketSource(destinationBucket)
                                .originAccessIdentity(webOai)
                                .build()
                        )
                        .behaviors(
                            listOf(
                                Behavior.builder().isDefaultBehavior(true).lambdaFunctionAssociations(
                                    listOf(
                                        LambdaFunctionAssociation.builder()
                                            .eventType(LambdaEdgeEventType.VIEWER_REQUEST)
                                            .lambdaFunction(
                                                lambdaEdgeFunction.currentVersion
                                            )
                                            .build()
                                    )
                                ).build()
                            )
                        )
                        .build()
                )
            )
            .priceClass(PriceClass.PRICE_CLASS_100)
            .loggingConfig(
                LoggingConfiguration.builder()
                    .bucket(
                        Bucket.Builder.create(stack, "cantilever-CloudFrontLogs")
                            .accessControl(BucketAccessControl.LOG_DELIVERY_WRITE)
                            .versioned(false)
                            .removalPolicy(RemovalPolicy.DESTROY)
                            .build()
                    )
                    .includeCookies(true)
                    .build()
            )
            .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
            .errorConfigurations(
                listOf(
                    CfnDistribution.CustomErrorResponseProperty.builder()
                        .errorCode(403)
                        .responseCode(200)
                        .responsePagePath("/index.html") // TODO: this should be a 403 page
                        .build(),
                    CfnDistribution.CustomErrorResponseProperty.builder()
                        .errorCode(404)
                        .responseCode(200)
                        .responsePagePath("/index.html") // TODO: this should be a 404 page
                        .build()
                )
            )
           /* .viewerCertificate(
                ViewerCertificate.fromAcmCertificate(
                    Certificate.fromCertificateArn(
                        stack,
                        "9b8f27c6-87be-4c14-a368-e6ad3ac4fb68",
                        "arn:aws:acm:us-east-1:086949310404:certificate/9b8f27c6-87be-4c14-a368-e6ad3ac4fb68"
                    ),
                    ViewerCertificateOptions.builder()
                        .aliases(listOf("www.cantilevers.org"))
                        .securityPolicy(SecurityPolicyProtocol.TLS_V1_2_2021)
                        .build()
                )
            )*/
            .build()
    }

    /**
     *
     * DnsValidatedCertificate websiteCertificate = DnsValidatedCertificate.Builder.create(this, "WebsiteCertificate")
     *                                                                             .hostedZone(hostedZone)
     *                                                                             .region("us-east-1")
     *                                                                             .domainName(stackConfig.getDomainName())
     *                                                                             .subjectAlternativeNames(List.of(String.format("www.%s", stackConfig.getDomainName())))
     *                                                                             .build();
     *
     * SET UP www REDIRECT
     *
     * HttpsRedirect webHttpsRedirect = HttpsRedirect.Builder.create(this, "WebHttpsRedirect")
     *                                                       .certificate(websiteCertificate)
     *                                                       .recordNames(List.of(String.format("www.%s", stackConfig.getDomainName())))
     *                                                       .targetDomain(stackConfig.getDomainName())
     *                                                       .zone(hostedZone)
     *                                                       .build();
     *
     * ARecord apexARecord = ARecord.Builder.create(this, "ApexARecord")
     *                                      .recordName(stackConfig.getDomainName())
     *                                      .zone(hostedZone)
     *                                      .target(RecordTarget.fromAlias(new CloudFrontTarget(cloudFrontWebDistribution)))
     *                                      .build();
     */
}