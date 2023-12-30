package org.liamjd.cantilever.lambda.image

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ImgRes
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region

/**
 * Respond to the SQSEvent which will contain the S3 key of the image file to resize
 */
class ImageProcessorHandler : RequestHandler<SQSEvent, String> {

    private val s3Service: S3Service
    private val sqsService: SQSService

    private lateinit var logger: LambdaLogger
    private lateinit var processor: ImageProcessor

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
        sqsService = SQSServiceImpl(Region.EU_WEST_2)

    }

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val destinationBucket = System.getenv("destination_bucket")
        logger = context.logger
        processor = ImageProcessor(logger)

        var response = "200 OK"

        logger.info("Received ${event.records.size} events received for image processing")

        event.records.forEach { eventRecord ->
            logger.info("Event record: ${eventRecord.body}")

            when (val sqsMsg = Json.decodeFromString<ImageSQSMessage>(eventRecord.body)) {
                is ImageSQSMessage.ResizeImageMsg -> {
                    response = processImageResize(sqsMsg, sourceBucket)
                }
                is ImageSQSMessage.CopyImagesMsg -> {
                    logger.info("Received request to copy images from source to destination bucket")
                    response =  processImageCopy(sqsMsg, sourceBucket, destinationBucket)
                }
            }
        }
        return response
    }

    /**
     * Process the image resize message. For each resolution defined in the project metadata, create a new image based on the original
     * @param imageMessage the SQS message containing the image to resize
     * @param sourceBucket the bucket containing the image to resize
     * @return a String response to the SQS message
     */
    private fun processImageResize(
        imageMessage: ImageSQSMessage.ResizeImageMsg, sourceBucket: String
    ): String {
        var responseString = "200 OK"

        try {
            val projectString = s3Service.getObjectAsString(S3_KEY.projectKey, sourceBucket)
            val project = Yaml.default.decodeFromString(CantileverProject.serializer(), projectString)
            logger.info("Project: $project")

            // loop through all the image resolutions and create resized images for each uploaded file
            if (project.imageResolutions.isNotEmpty()) {
                logger.info("Checking if image exists in $sourceBucket/${imageMessage.metadata.srcKey}")
                if (s3Service.objectExists(imageMessage.metadata.srcKey, sourceBucket)) {
                    val imageBytes = s3Service.getObjectAsBytes(imageMessage.metadata.srcKey, sourceBucket)
                    val contentType = s3Service.getContentType(imageMessage.metadata.srcKey, sourceBucket)
                    if (imageBytes.isNotEmpty()) {
                        logger.info("Resize image: ${imageBytes.size} bytes")
                        project.imageResolutions.forEach { (name, imgRes) ->
                            logger.info("Resize image: $name (${imgRes.w}x${imgRes.h})")
                            if (imgRes.w == null && imgRes.h == null) {
                                logger.info("Skipping image resize for $name as no dimensions specified")
                            } else {
                                val resizedBytes =
                                    processor.resizeImage(imgRes, imageBytes, getFormatNameFromContentType(contentType))
                                val destKey = calculateFilename(imageMessage, name)
                                logger.info("Resize image: writing $destKey (${resizedBytes.size} bytes) to $sourceBucket")
                                s3Service.putObjectAsBytes(
                                    destKey, sourceBucket, resizedBytes, contentType ?: "image/jpeg"
                                )
                            }
                        }
                        // finally, copy the original image to the generated folder, unchanged
                        logger.info("Copying original image to generated folder")
                        s3Service.copyObject(
                            imageMessage.metadata.srcKey,
                            calculateFilename(imageMessage, null),
                            sourceBucket
                        )
                        logger.info("Creating internal thumbnail 100x100")
                        val thumbNailRes = ImgRes(100, 100)
                        val resizedBytes =
                            processor.resizeImage(thumbNailRes, imageBytes, getFormatNameFromContentType(contentType))
                        val destKey = calculateFilename(imageMessage, S3_KEY.thumbnail)
                        logger.info("Resize image: writing $destKey (${resizedBytes.size} bytes) to $sourceBucket")
                        s3Service.putObjectAsBytes(
                            destKey, sourceBucket, resizedBytes, contentType ?: "image/jpeg"
                        )
                    } else {
                        logger.error("Resize image: ${imageMessage.metadata.srcKey} is empty")
                        return "500 Internal Server Error"
                    }
                }
            } else {
                logger.warn("No  image resolutions defined in project metadata. Copying image to destination bucket without resizing")
                // TODO: copy image to destination bucket
                return "202 Accepted"
            }


        } catch (e: Exception) {
            logger.error("Failed to process image file; ${e.message}")
            responseString = "500 Internal Server Error"
        }

        return responseString
    }

    private fun processImageCopy(imageMessage: ImageSQSMessage.CopyImagesMsg, sourceBucket: String, destinationBucket: String): String {
        var responseString = "200 OK"

        try {
            imageMessage.imageList.forEach { imageKey ->
                // imageKey will be as requested by the markdown file, e.g. /images/my-image.jpg
                // sourceKey will be the full path in the source bucket, e.g. generated/images/my-image.jpg
                // and destination key will be the full path in the destination bucket, e.g. images/my-image.jpg (no leading slash)
                // TODO: this requires the user to always request images in a folder called images.
                val sourceKey = "${S3_KEY.generated}${imageKey}"
                val destinationKey = imageKey.removePrefix("/")
                logger.info("Copying $sourceKey to $destinationKey, from $sourceBucket to $destinationBucket")
                val copyResult = s3Service.copyObject(sourceKey, destinationKey, sourceBucket, destinationBucket)
                if(copyResult == -1) {
                    logger.error("Failed to copy $sourceKey to $destinationKey, from $sourceBucket to $destinationBucket")
                    responseString = "500 Internal Server Error"
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to copy images; ${e.message}")
            responseString = "500 Internal Server Error"
        }

        return responseString
    }

    /**
     * Calculate the filename for the resized image
     * Based on the original file name, and the resolution name
     */
    private fun calculateFilename(
        imageMessage: ImageSQSMessage.ResizeImageMsg, resName: String?
    ): String {
        val origSuffix = imageMessage.metadata.srcKey.substringAfterLast(".")
        val newPrefix = S3_KEY.generated + "/"
        val leafName = imageMessage.metadata.srcKey.removePrefix(S3_KEY.sourcesPrefix).substringBeforeLast(".")
        val finalResName = if (resName != null) {
            "/$resName."
        } else "."
        return "$newPrefix$leafName${finalResName}${origSuffix}"
    }

    /**
     * Get the format name from the content type
     * The Java ImageIO file writer needs an "informal format name" to write the image, but I only have the content type
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
}

/**
 * Wrappers for logging to make it slightly less annoying
 */
fun LambdaLogger.info(function: String, message: String) = log("INFO: $function:  $message\n")
fun LambdaLogger.info(message: String) = info("ImageProcessorHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN: $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("ImageProcessorHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR: $function:  $message\n")
fun LambdaLogger.error(message: String) = error("ImageProcessorHandler", message)