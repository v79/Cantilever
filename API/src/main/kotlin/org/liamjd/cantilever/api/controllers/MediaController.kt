package org.liamjd.cantilever.api.controllers

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ImageDTO
import org.liamjd.cantilever.models.rest.ImageListDTO
import java.net.URLDecoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Load, save and delete Images from the S3 bucket
 */
class MediaController(sourceBucket: String, generationBucket: String) : KoinComponent,
    APIController(sourceBucket, generationBucket) {

    /**
     * Return a list of all the images
     * @return [ImageListDTO] object containing the list of images, a count and the last updated date/time
     */
    fun getImageList(request: Request<Unit>): Response<APIResult<ImageListDTO>> {
        val domain = request.headers["cantilever-project-domain"]!!
        val imageList = runBlocking {
            val images = dynamoDBService.listAllNodesForProject(domain, SOURCE_TYPE.Images)
                .filterIsInstance<ContentNode.ImageNode>()
            info("Loaded ${images.size} images")
            val sorted = images.sortedByDescending { it.srcKey }
            sorted.forEach { println(it.srcKey) }
            ImageListDTO(
                count = sorted.size,
                lastUpdated = Clock.System.now(),
                images = sorted
            )
        }
        return Response.ok(body = APIResult.Success(value = imageList))
    }

    /**
     * Load an image file with the specified `srcKey` and specified `resolution` and return it as a byte array [ImageDTO] response
     * If resolution is not specified, return the original image
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun getImage(request: Request<Unit>): Response<APIResult<ImageDTO>> {
        val srcKey =
            request.pathParameters["srcKey"] ?: return Response.badRequest(APIResult.Error("No srcKey provided"))
        val decodedKey = URLDecoder.decode(srcKey, Charsets.UTF_8)
        val resolution = request.pathParameters["resolution"]
        info("Fetching image $decodedKey at resolution $resolution")
        // srcKey will be /sources/images/<image-name>.<ext>, so we need to strip off the /sources/images/ prefix and add the /generated/images/ prefix
        // I also need to move the <ext> to the end of the generated key
        val ext = decodedKey.substringAfterLast(".")
        val generatedKey = decodedKey
            .replace("sources/images/", "generated/images/") + if (resolution != null) {
            "/${resolution}.$ext"
        } else {
            ".${ext}"
        }
        if (s3Service.objectExists(generatedKey, generationBucket)) {
            val image = s3Service.getObjectAsBytes(generatedKey, generationBucket)
            val encoded = Base64.encode(image)
            return Response.ok(
                APIResult.Success(
                    value = ImageDTO(
                        srcKey = srcKey,
                        getContentTypeFromExtension(ext),
                        bytes = encoded
                    )
                )
            )
        } else {
            error("Image '$generatedKey' not found")
            return Response.notFound(APIResult.Error("Image '$generatedKey' not found"))
        }
    }

    /**
     * Upload an image to the S3 bucket
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun uploadImage(request: Request<ImageDTO>): Response<APIResult<ImageDTO>> {
        val domain = request.headers["cantilever-project-domain"]!!

        val imageBody = request.body
        val srcKey = "$domain/sources/images/${imageBody.srcKey}"
        val contentType = imageBody.contentType
        var dto: ImageDTO?
        try {
            val startIndex = imageBody.bytes.indexOf("base64,") + 7 // 7 is the length of the string "base64,"
            val bytes = Base64.decode(imageBody.bytes, startIndex)
            info("Uploading image $srcKey with ${bytes.size} bytes")
            s3Service.putObjectAsBytes(key = srcKey, bucket = sourceBucket, contentType = contentType, contents = bytes)
            dto = ImageDTO(
                srcKey,
                contentType,
                ""
            ) // we don't really need to return the bytes, the browser already has them
        } catch (e: Exception) {
            error("Error uploading image $srcKey: ${e.message}")
            return Response.badRequest(APIResult.Error("Error uploading image $srcKey: ${e.message}"))
        }
        return Response.ok(APIResult.Success(dto))
    }

    /**
     * Delete an image from the `sources` bucket, and remove all the generated variants
     * This does NOT delete any images from the destination website bucket
     */
    fun deleteImage(request: Request<Unit>): Response<APIResult<String>> {
        val srcKey =
            request.pathParameters["srcKey"] ?: return Response.badRequest(APIResult.Error("No srcKey provided"))
        val decodedKey = URLDecoder.decode(srcKey, Charsets.UTF_8)

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
        return Response.ok(APIResult.Success(value = "Image'${srcKey}' deleted successfully"))
    }


    /**
     * Get the content type from the file extension
     * Defaults to image/jpeg if the extension is not recognised
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