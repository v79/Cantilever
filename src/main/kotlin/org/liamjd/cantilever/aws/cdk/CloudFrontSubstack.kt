package org.liamjd.cantilever.aws.cdk

import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.cloudfront.*
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

    fun createCloudfrontDistribution(stack: Stack, sourceBucket: IBucket, destinationBucket: IBucket) {

        val webOai = OriginAccessIdentity.Builder.create(stack, "WebOai").build()
        destinationBucket.grantRead(webOai)

        /**
         * this bit goes after the comment, below, if I had a certificate set up
         *  .viewerCertificate(
         *                     ViewerCertificate.fromAcmCertificate(
         *                         websiteCertificate, ViewerCertificateOptions.builder()
         *                             .aliases(List.of(stackConfig.getDomainName()))
         *                             .build()
         *                     )
         *                 )
         */

        val cloudFrontWebDistribution: CloudFrontWebDistribution =
            CloudFrontWebDistribution.Builder.create(stack, "cantilever-CloudFrontWebDistribution")
                .comment(java.lang.String.format("CloudFront distribution for cantilever"))

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