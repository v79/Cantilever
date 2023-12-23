package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.rest.ImageListDTO
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity

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

    override fun info(message: String) = println("INFO: MediaController: $message")
    override fun warn(message: String) = println("WARN: MediaController: $message")
    override fun error(message: String) = println("ERROR: MediaController: $message")
}