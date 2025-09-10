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
     * The request URL will not match the actual S3 key, so we need to convert it
     * Example request URL: www.cantilevers.org%2Fsources%2Fimages%2Finchinnan-bascule-bridge.jpg/__thumb
     * Actual S3 key: www.cantilevers.org/generated/images/inchinnan-bascule-bridge/__thumb.jpg
     * If resolution is not specified, return the original image
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun getImage(request: Request<Unit>): Response<APIResult<ImageDTO>> {
        val srcKey =
            request.pathParameters["srcKey"] ?: return Response.badRequest(APIResult.Error("No srcKey provided"))
        val decodedKey = URLDecoder.decode(srcKey, Charsets.UTF_8)
        var resolution = request.pathParameters["resolution"]
        // Resolution might be provided as "__thumb" in the path; normalise to just "thumb"
        if (resolution != null && resolution.startsWith("__")) {
            resolution = resolution.removePrefix("__")
        }
        info("Fetching image $decodedKey at resolution $resolution")
        val ext = decodedKey.substringAfterLast(".").lowercase()
        val generatedKey = calculateGeneratedKey(decodedKey, ext, resolution)
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
        val domain = request.headers["cantilever-project-domain"]!!

        info("Deleting image $decodedKey and all its generated versions")
        s3Service.deleteObject(decodedKey, sourceBucket)
        val ext = decodedKey.substringAfterLast(".")

        // TODO: Do I really want to delete all the generated images? What if the user has used them in a blog post?
        runBlocking {
            val project = dynamoDBService.getProject(domain)
            project?.imageResolutions?.forEach { resolution ->
                val resolutionKey = decodedKey.replace(S3_KEY.imagesPrefix, S3_KEY.generatedImagesPrefix)
                    .removeSuffix(".$ext") + "/${resolution.key}.$ext"
                info("Deleting generated image $resolutionKey")
                s3Service.deleteObject(resolutionKey, sourceBucket)
            }
        }
        s3Service.deleteObject(decodedKey.replaceFirst("sources", "generated"), sourceBucket)
        s3Service.deleteObject(
            decodedKey.replace(S3_KEY.imagesPrefix, S3_KEY.generatedImagesPrefix)
                .removeSuffix(".$ext") + "/${S3_KEY.thumbnail}.$ext", sourceBucket
        )
        return Response.ok(APIResult.Success(value = "Image'${srcKey}' deleted successfully"))
    }

    /**
     * Converts a source image key into a generated image key based on specified parameters.
     *
     * The method modifies the input `srcKey` to replace the base path "sources/images/"
     * with "generated/images/". If a valid `resolution` is provided, it appends the
     * resolution to the file name before the extension. If `resolution` is not provided,
     * the generated key remains unchanged except for the base path replacement.
     *
     * @param srcKey the original source image key, typically including its path and filename
     * @param ext the file extension of the image (e.g., "jpg", "png")
     * @param resolution an optional parameter specifying the desired resolution; can be null or blank
     * @return the generated key derived from the source key and resolution
     */
    internal fun calculateGeneratedKey(srcKey: String, ext: String, resolution: String?): String {
        // Convert sources path to generated path
        val generatedBase = srcKey.replace("sources/images/", "generated/images/")
        return if (resolution.isNullOrBlank()) {
            // No resolution: just return the base generated path with original filename
            generatedBase
        } else {
            // Insert /__<resolution> before the file extension and keep folders
            val withoutExt = generatedBase.removeSuffix(".$ext")
            "$withoutExt/__${resolution}.$ext"
        }
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