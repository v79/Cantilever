package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ImageDTO
import org.liamjd.cantilever.models.rest.ImageListDTO
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import java.net.URLDecoder
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
        val decodedKey = URLDecoder.decode(srcKey, Charsets.UTF_8)
        val resolution = request.pathParameters["resolution"]
        info("Fetching image $decodedKey at resolution $resolution")
        // srcKey will be /sources/images/<image-name>.<ext> so we need to strip off the /sources/images/ prefix and add the /generated/images/ prefix
        // I also need to move the <ext> to the end of the generated key
        val ext = decodedKey.substringAfterLast(".")
        val generatedKey = decodedKey.removeSuffix(".${ext}")
            .replace(S3_KEY.imagesPrefix, S3_KEY.generatedImagesPrefix) + if (resolution != null) {
            "/${resolution}.$ext"
        } else {
            ".${ext}"
        }
        val image = s3Service.getObjectAsBytes(generatedKey, sourceBucket)

        // need to base64 encode the image
        val encoded = Base64.encode(image)
        return ResponseEntity.ok(
            APIResult.Success(
                value = ImageDTO(
                    srcKey = srcKey,
                    getContentTypeFromExtension(ext),
                    bytes = encoded
                )
            )
        )
    }

    /**
     * Upload an image to the S3 bucket
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun uploadImage(request: Request<ImageDTO>): ResponseEntity<APIResult<ImageDTO>> {
        loadContentTree()

        val imageBody = request.body
        val srcKey = "sources/images/${imageBody.srcKey}"
        val contentType = imageBody.contentType
        var dto: ImageDTO? = null
        try {
            val startIndex = imageBody.bytes.indexOf("base64,") + 7 // 7 is the length of the string "base64,"
            val bytes = Base64.decode(imageBody.bytes, startIndex)
            info("Uploading image $srcKey with ${bytes.size} bytes")
            s3Service.putObjectAsBytes(key = srcKey, bucket = sourceBucket, contentType = contentType, contents = bytes)

            // add the image to the content tree
            val metadata = ContentMetaDataBuilder.ImageBuilder.buildFromSourceString("", srcKey)
            dto = ImageDTO(srcKey, contentType, "") // we don't really need to return the bytes, the browser already has them
            contentTree.insertImage(metadata)
            saveContentTree()
        } catch (e: Exception) {
            error("Error uploading image $srcKey: ${e.message}")
            return ResponseEntity.badRequest(APIResult.Error("Error uploading image $srcKey: ${e.message}"))
        }
        return ResponseEntity.ok(APIResult.Success(dto))
    }

    /**
     * Delete an image from the sources bucket, and remove all the generated variants
     * This does NOT delete any images from the destination website bucket
     */
    fun deleteImage(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val srcKey =
            request.pathParameters["srcKey"] ?: return ResponseEntity.badRequest(APIResult.Error("No srcKey provided"))
        val decodedKey = URLDecoder.decode(srcKey, Charsets.UTF_8)
        loadContentTree()
        loadProjectDefinition()

        info("Deleting image $decodedKey and all its generated versions")
        s3Service.deleteObject(decodedKey, sourceBucket)
        val ext = decodedKey.substringAfterLast(".")

        // TODO: Do I really want to delete all the generated images? What if the user has used them in a blog post?
        project.imageResolutions.forEach { resolution ->
            val resolutionKey = decodedKey.replace(S3_KEY.imagesPrefix, S3_KEY.generatedImagesPrefix)
                .removeSuffix(".$ext") + "/${resolution.key}.$ext"
            info("Deleting generated image $resolutionKey")
            s3Service.deleteObject(resolutionKey, sourceBucket)
        }
        s3Service.deleteObject(decodedKey.replaceFirst("sources", "generated"), sourceBucket)
        s3Service.deleteObject(
            decodedKey.replace(S3_KEY.imagesPrefix, S3_KEY.generatedImagesPrefix)
                .removeSuffix(".$ext") + "/${S3_KEY.thumbnail}.$ext", sourceBucket
        )

        contentTree.deleteImage(ContentMetaDataBuilder.ImageBuilder.buildFromSourceString("", decodedKey))
        println("Content tree now has ${contentTree.images.size} images")
        saveContentTree()

        return ResponseEntity.ok(APIResult.Success(value = "Image'${srcKey}' deleted successfully"))
    }


    /**
     * Get the content type from the file extension
     */
    private fun getContentTypeFromExtension(extension: String): String {
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }

    override fun info(message: String) = println("INFO: MediaController: $message")
    override fun warn(message: String) = println("WARN: MediaController: $message")
    override fun error(message: String) = println("ERROR: MediaController: $message")
}