package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.Duration
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.Tags
import software.amazon.awscdk.services.cloudfront.*
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.BucketAccessControl
import software.amazon.awscdk.services.s3.IBucket

class CloudFrontSubstack {

    // taken from https://johntipper.org/a-static-website-with-api-backend-using-aws-cdk-and-java/

    // Route53 hosted zone created out-of-band, e.g already existing
    // but this requires the environment to be configured in the stack, which means putting the account ID in git

    /*        val hostedZone = HostedZone.fromLookup(
                this, "HostedZone", HostedZoneProviderProps.builder()
                    .domainName("cantilevers.org")
                    .build()
            )*/

    /**
     * This isn't complete, there are extra steps I need to do at the AWS console:
     * - Attach the SSL certificate
     * - Add the alternate domain name (www...)
     */
    fun createCloudfrontDistribution(
        stack: Stack,
        sourceBucket: IBucket,
        destinationBucket: IBucket
    ): CloudFrontWebDistribution {

        Tags.of(stack).add("Cantilever", "v0.0.5")

        val webOai = OriginAccessIdentity.Builder.create(stack, "WebOai").build()
        destinationBucket.grantRead(webOai)

        return CloudFrontWebDistribution.Builder.create(stack, "cantilever-CloudFrontWebDistribution")
            .comment(String.format("CloudFront distribution for cantilever"))
            .defaultRootObject("index.html")
            .originConfigs(
                listOf(
                    SourceConfiguration.builder()
                        .behaviors(
                            listOf(
                                Behavior.builder()
                                    .isDefaultBehavior(true)
                                    .defaultTtl(Duration.minutes(5))
                                    .maxTtl(Duration.minutes(5))
                                    .build()
                            )
                        )
                        .s3OriginSource(
                            S3OriginConfig.builder()
                                .originAccessIdentity(webOai)
                                .s3BucketSource(destinationBucket)
                                .build()
                        )
                        .build()
                )
            )
            .priceClass(PriceClass.PRICE_CLASS_100)
            .loggingConfig(LoggingConfiguration.builder()
                .bucket(Bucket.Builder.create(stack, "cantilever-CloudFrontLogs")
                    .accessControl(BucketAccessControl.LOG_DELIVERY_WRITE)
                    .versioned(false)
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .build())
                .includeCookies(true)
                .build()
            )
            .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
            .errorConfigurations(
                listOf(
                    CfnDistribution.CustomErrorResponseProperty.builder()
                        .errorCode(403)
                        .responseCode(200)
                        .responsePagePath("/index.html")
                        .build(),
                    CfnDistribution.CustomErrorResponseProperty.builder()
                        .errorCode(404)
                        .responseCode(200)
                        .responsePagePath("/index.html")
                        .build()
                )
            )
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
     *
     *                                                       .certificate(websiteCertificate)
     *
     *                                                       .recordNames(List.of(String.format("www.%s", stackConfig.getDomainName())))
     *
     *                                                       .targetDomain(stackConfig.getDomainName())
     *
     *                                                       .zone(hostedZone)
     *
     *                                                       .build();
     *
     * ARecord apexARecord = ARecord.Builder.create(this, "ApexARecord")
     *
     *                                      .recordName(stackConfig.getDomainName())
     *
     *                                      .zone(hostedZone)
     *
     *                                      .target(RecordTarget.fromAlias(new CloudFrontTarget(cloudFrontWebDistribution)))
     *
     *                                      .build();
     */
}