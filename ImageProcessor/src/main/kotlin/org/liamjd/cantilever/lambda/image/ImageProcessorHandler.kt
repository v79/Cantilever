package org.liamjd.cantilever.lambda.image

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.CantileverProject
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
    private val processor: ImageProcessor
    private lateinit var logger: LambdaLogger

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
        sqsService = SQSServiceImpl(Region.EU_WEST_2)
        processor = ImageProcessor()
    }

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        logger = context.logger
        var response = "200 OK"

        logger.info("Received ${event.records.size} events received for image processing")

        event.records.forEach { eventRecord ->
            logger.info("Event record: ${eventRecord.body}")

            when (val sqsMsg = Json.decodeFromString<ImageSQSMessage>(eventRecord.body)) {
                is ImageSQSMessage.ResizeImageMsg -> {
                    response = processImageResize(sqsMsg, sourceBucket)
                }
            }
        }
        return response
    }

    private fun processImageResize(
        imageMessage: ImageSQSMessage.ResizeImageMsg,
        sourceBucket: String
    ): String {
        var responseString = "200 OK"

        try {
            val projectString = s3Service.getObjectAsString(S3_KEY.projectKey, sourceBucket)
            val project = Yaml.default.decodeFromString(CantileverProject.serializer(), projectString)
            logger.info("Project: $project")

            // loop through all the image resolutions and create resized images for each uploaded file
            if (project.imageResolutions.isNotEmpty()) {
                logger.info("Checking if image exists in $sourceBucket/${imageMessage.srcKey}")
                if (s3Service.objectExists(imageMessage.srcKey, sourceBucket)) {
                    val imageBytes = s3Service.getObjectAsBytes(imageMessage.srcKey,sourceBucket)
                    if (imageBytes.isNotEmpty()) {
                        logger.info("ImageProcessorHandler: resizeImage: ${imageBytes.size} bytes")
                        project.imageResolutions.forEach { (name, imgRes) ->
                            logger.info("ImageProcessorHandler: resizeImage: $name ${imgRes.w}x${imgRes.h}")
                            if (imgRes.w == null && imgRes.h == null) {
                                logger.info("Skipping image resize for $name as no dimensions specified")
                            } else {
                                val resizedBytes = processor.resizeImage(imgRes, imageBytes, imageMessage.srcKey)

                                val destKey =
                                    "${S3_KEY.generated}/${imageMessage.srcKey.removePrefix(S3_KEY.sourcesPrefix)}-${name}"
                                s3Service.putObject(destKey, sourceBucket, String(resizedBytes), "image/jpeg")
                            }
                        }
                } else {
                    logger.error("ImageProcessorHandler: resizeImage: ${imageMessage.srcKey} is empty")
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