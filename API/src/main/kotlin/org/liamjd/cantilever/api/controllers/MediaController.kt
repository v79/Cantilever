package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.ImageDTO
import org.liamjd.cantilever.models.rest.ImageListDTO
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Load, save and delete Images from the S3 bucket
 */
class MediaController(sourceBucket: String) : KoinComponent, APIController(sourceBucket) {

    /**
     * Return a list of all the images in the content tree
     * @return [ImageListDTO] object containing the list of images, a count and the last updated date/time
     */
    fun getImages(request: Request<Unit>): ResponseEntity<APIResult<ImageListDTO>> {
        return if (s3Service.objectExists(S3_KEY.metadataKey, sourceBucket)) {
            loadContentTree()
            info("Fetching all images from metadata.json")
            val lastUpdated = s3Service.getUpdatedTime(S3_KEY.metadataKey, sourceBucket)
            val images = contentTree.images.toList()
            info("Loaded ${images.size} images")
            val sorted = images.sortedByDescending { it.srcKey }
            sorted.forEach { println(it.srcKey) }
            val imageList = ImageListDTO(
                count = sorted.size,
                lastUpdated = lastUpdated,
                images = sorted
            )
            ResponseEntity.ok(body = APIResult.Success(value = imageList))
        } else {
            error("Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket")
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket"))
        }
    }

    /**
     * Load an image file with the specified `srcKey` and specified `resolution` and return it as [ImageDTO] response
     * If resolution is not specified, return the original image
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun getImage(request: Request<Unit>): ResponseEntity<APIResult<ImageDTO>> {
        val srcKey =
            request.pathParameters["srcKey"] ?: return ResponseEntity.badRequest(APIResult.Error("No srcKey provided"))
        val resolution = request.pathParameters["resolution"]
        info("Loading image $srcKey at resolution $resolution")
        // srcKey will be /sources/images/<image-name>.<ext> so we need to strip off the /sources/images/ prefix and add the /generated/images/ prefix
        val generatedKey = srcKey.replace(S3_KEY.imagesPrefix, S3_KEY.generatedImagesPrefix) + if (resolution != null) {
            "/$resolution"
        } else {
            ""
        }
        info("Generated key is $generatedKey")
        val image = s3Service.getObjectAsBytes(generatedKey, sourceBucket)
        // need to base64 encode the image
        val encoded = Base64.encode(image)
        return ResponseEntity.ok(APIResult.Success(value = ImageDTO(srcKey = srcKey, bytes = encoded)))
    }

    override fun info(message: String) = println("INFO: MediaController: $message")
    override fun warn(message: String) = println("WARN: MediaController: $message")
    override fun error(message: String) = println("ERROR: MediaController: $message")
}