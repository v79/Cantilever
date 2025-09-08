package org.liamjd.cantilever.lambda.image

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.liamjd.cantilever.common.EnvironmentProvider
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.SystemEnvironmentProvider
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ImgRes
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.services.AWSLogger
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.DynamoDBServiceImpl
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

/**
 * Set up dependency injection
 */
val imageProcessorModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
    single<DynamoDBService> {
        DynamoDBServiceImpl(
            region = Region.EU_WEST_2, enableLogging = true, dynamoDbClient = DynamoDbAsyncClient.create()
        )
    }
}

/**
 * This object is used to set up Koin dependency injection. It ensures that Koin is only started once, even in unit testing scenarios.
 */
object KoinSetup {
    fun setup(modules: List<org.koin.core.module.Module>) {
        // Only start Koin if it's not already running
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(imageProcessorModule)
            }
        }
    }
}

/**
 * Respond to the SQSEvent, which will contain the S3 key of the image file to resize
 */
@Suppress("unused")
class ImageProcessorHandler(private val environmentProvider: EnvironmentProvider = SystemEnvironmentProvider()) :
    RequestHandler<SQSEvent, String>, KoinComponent,
    AWSLogger(enableLogging = true, msgSource = "ImageProcessorHandler") {

    init {
        KoinSetup.setup(listOf(imageProcessorModule))
    }

    private val s3Service: S3Service by inject()
    private val sqsService: SQSService by inject()
    private val dynamoDBService: DynamoDBService by inject()
    private lateinit var processor: ImageProcessor
    override var logger: LambdaLogger? = null

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = environmentProvider.getEnv("source_bucket")
        val destinationBucket = environmentProvider.getEnv("destination_bucket")
        val generationBucket = environmentProvider.getEnv("generation_bucket")
        logger = context.logger
        processor = ImageProcessor(logger)

        var response = "200 OK"

        log("Received ${event.records.size} events received for image processing")

        try {
            event.records.forEach { eventRecord ->
                log("Event record: ${eventRecord.body}")

                when (val sqsMsg = Json.decodeFromString<ImageSQSMessage>(eventRecord.body)) {
                    is ImageSQSMessage.ResizeImageMsg -> {
                        log("Received request to resize image ${sqsMsg.metadata.srcKey} in $sourceBucket and write to $generationBucket bucket")
                        response = processImageResize(sqsMsg, sourceBucket, generationBucket)
                    }

                    is ImageSQSMessage.CopyImagesMsg -> {
                        log("Received request to copy images from $generationBucket to $destinationBucket bucket")
                        response = processImageCopy(sqsMsg, generationBucket, destinationBucket)
                    }
                }
            }
            return response
        } catch (e: Exception) {
            log("ERROR", "Failed to process image file; ${e.message}")
            return "500 Internal Server Error"
        }
    }

    /**
     * Process the image resize Message. For each resolution defined in the project metadata, create a new image based on the original
     * @param imageMessage the SQS message containing the image to resize
     * @param sourceBucket the bucket containing the image to resize
     * @param generationBucket the bucket to write the resized images to
     * @return a String response to the SQS message
     */
    private fun processImageResize(
        imageMessage: ImageSQSMessage.ResizeImageMsg, sourceBucket: String, generationBucket: String
    ): String {
        var responseString = "200 OK"

        try {
            val domain = imageMessage.projectDomain
            val project = getProjectModel(domain) ?: throw Exception("Project model is null")

            log("Checking if image exists in $sourceBucket/${imageMessage.metadata.srcKey}")
            if (s3Service.objectExists(imageMessage.metadata.srcKey, sourceBucket)) {
                // loop through all the image resolutions and create resized images for each uploaded file
                val imageBytes = s3Service.getObjectAsBytes(imageMessage.metadata.srcKey, sourceBucket)
                val contentType = s3Service.getContentType(imageMessage.metadata.srcKey, sourceBucket)
                if (imageBytes.isNotEmpty()) {
                    if (project.imageResolutions.isNotEmpty()) {
                        log("Resize image: ${imageBytes.size} bytes")
                        project.imageResolutions.forEach { (name, imgRes) ->
                            log("Resize image: $name (${imgRes.w}x${imgRes.h})")
                            if (imgRes.w == null && imgRes.h == null) {
                                log("Skipping image resize for $name as no dimensions specified")
                            } else {
                                val resizedBytes =
                                    processor.resizeImage(imgRes, imageBytes, getFormatNameFromContentType(contentType))
                                val destKey = imageMessage.metadata.calculateDestinationKey(domain)
                                log("Resize image: writing $destKey (${resizedBytes.size} bytes) to $sourceBucket")
                                s3Service.putObjectAsBytes(
                                    destKey, generationBucket, resizedBytes, contentType ?: "image/jpeg"
                                )
                            }
                        }
                        // finally, copy the original image to the generated folder, unchanged
                        log("Copying original image to generated folder")
                        val copyToKey =
                            "$domain/generated/images/${imageMessage.metadata.srcKey.substringAfterLast("/")}"
                        s3Service.copyObject(
                            srcKey = imageMessage.metadata.srcKey,
                            destKey = copyToKey,
                            srcBucket = sourceBucket,
                            destBucket = generationBucket
                        )
                    } else {
                        log(
                            "WARN",
                            "No image resolutions defined in project metadata. Copying image to destination bucket without resizing"
                        )
                        // CopyImagesMsg has a unique requirement for the src key, in that it matches that which would be specified in the Markdown
                        // For example, Markdown would specify /assets/my-image.jpg which is very different from metadata.srcKey
                        val copyMsg = ImageSQSMessage.CopyImagesMsg(
                            imageMessage.projectDomain,
                            listOf(imageMessage.metadata.srcKey.removePrefix(domain).removePrefix("sources/images"))
                        )
                        processImageCopy(copyMsg, sourceBucket, generationBucket)
                    }
                    createThumbnailFromImage(imageBytes, contentType, imageMessage, domain, generationBucket)
                    return "202 Accepted"
                } else {
                    log("ERROR", "Resize image: ${imageMessage.metadata.srcKey} is empty")
                    return "500 Internal Server Error"
                }
            } else {
                log("ERROR", "Image ${imageMessage.metadata.srcKey} does not exist in bucket $sourceBucket")
                return "404 Not Found"
            }
        } catch (e: Exception) {
            log("ERROR", "Failed to process image file; ${e.message}")
            responseString = "500 Internal Server Error"
        }

        return responseString
    }

    /**
     * Create a 100x100 thumbnail from the original image and store it in a __thumb.jpg file in the image folder
     * e.g. domain/generated/images/my-image/__thumb.jpg
     * @param imageBytes the original image bytes
     * @param contentType the content type of the original image
     * @param imageMessage the SQS message containing the image metadata
     * @param domain the project domain
     * @param generationBucket the bucket to write the thumbnail to
     */
    private fun createThumbnailFromImage(
        imageBytes: ByteArray,
        contentType: String?,
        imageMessage: ImageSQSMessage.ResizeImageMsg,
        domain: String,
        generationBucket: String
    ) {
        log("Creating internal thumbnail 100x100")
        val thumbNailRes = ImgRes(100, 100)
        val resizedBytes = processor.resizeImage(thumbNailRes, imageBytes, getFormatNameFromContentType(contentType))
        // destKey will be in the format domain/generated/images/my-image.jpg
        // I need domain/generated/images/my-image/__thumb.jpg
        var destKey = imageMessage.metadata.calculateDestinationKey(domain)
        val suffix = destKey.substringAfterLast(".")
        destKey = destKey.removeSuffix(".${suffix}")
        destKey = destKey + "/" + S3_KEY.thumbnail + "." + suffix
        log("Resize image: writing $destKey (${resizedBytes.size} bytes) to $generationBucket")
        s3Service.putObjectAsBytes(
            destKey, generationBucket, resizedBytes, contentType ?: "image/jpeg"
        )
    }

    // TODO: Need stronger error handling here; there's a good chance that the source image won't exist
    /**
     * Process the image copy Message. Copy all uploaded images to the destination bucket
     * @param imageMessage the SQS message containing the image to resize
     * @param sourceBucket the bucket containing the image to resize
     * @param destinationBucket the bucket to write the resized images to
     * @return a String response to the SQS message
     */
    private fun processImageCopy(
        imageMessage: ImageSQSMessage.CopyImagesMsg, sourceBucket: String, destinationBucket: String
    ): String {
        var responseString = "200 OK"

        try {
            // load the project
            val domain = imageMessage.projectDomain
            val project = getProjectModel(domain) ?: throw Exception("Project model is null")
            imageMessage.imageList.forEach { imageKey ->
                // imageKey will be /sources/images/my-image.jpg
                val srcKey = "$domain$imageKey"
                // and destination key will be the full path in the destination bucket, e.g. domain.com/generated/images/my-image.jpg (no leading slash)
                // TODO: this requires the user to always request images in a folder called images.
                val destinationKey = project.domainKey + "generated" + imageKey.removePrefix("/sources/images")
                println("Copying $srcKey to $destinationKey, from $sourceBucket to $destinationBucket")
                val copyResult = s3Service.copyObject(srcKey, destinationKey, sourceBucket, destinationBucket)
                if (copyResult == -1) {
                    log(
                        "ERROR", "Failed to copy $imageKey to $destinationKey, from $sourceBucket to $destinationBucket"
                    )
                    responseString = "500 Internal Server Error"
                }
            }
        } catch (e: Exception) {
            log("ERROR", "Failed to copy images; ${e.message}")
            responseString = "500 Internal Server Error"
        }

        return responseString
    }

    /**
     * Calculate the filename for the resized image
     * Based on the original file name, and the resolution name
     * Will be in the format domain/generated/images/my-image.100x100.jpg
     * @param imageMessage the SQS message containing details of the image to resize
     * @param resName the name of the resolution to append to the filename
     *//*
        private fun calculateFilename(
            imageMessage: ImageSQSMessage.ResizeImageMsg, resName: String?
        ): String {
            // The original image key is in the format domain/sources/images/my-image.jpg
            // The destination image key should be in format domain/generated/images/my-image/100x100.jpg
            val domain = imageMessage.projectDomain
            val sourceLeafName = imageMessage.metadata.srcKey.substringAfterLast("/")
            val origSuffix = imageMessage.metadata.srcKey.substringAfterLast(".")
            val newPrefix = "$domain/generated/images/"
            val finalResName = if (resName != null) {
                "/$resName."
            } else "."
            return "$newPrefix$sourceLeafName${finalResName}${origSuffix}"
        }
    */

    /**
     * Get the format name from the content type
     * The Java ImageIO file writer needs an "informal format name" to write the image, but I only have the content type
     * @param contentType the MIME content type of the image
     * @return the informal format name for the image, or "jpg" if the content type is not recognised
     */
    private fun getFormatNameFromContentType(contentType: String?): String {
        return when (contentType?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg" // default to jpg
        }
    }

    /**
     * Return the CantileverProject model
     * @param domain the domain to load the project for
     * @return the CantileverProject model or null if it could not be loaded
     */
    private fun getProjectModel(domain: String): CantileverProject? {
        val project = runBlocking {
            try {
                dynamoDBService.getProject(domain)
            } catch (e: Exception) {
                log("ERROR", "Could not load project model for domain '$domain', exception: ${e.message}")
            }
        }
        return project as? CantileverProject?
    }
}
