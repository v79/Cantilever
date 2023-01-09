package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.toLocalDateTime
import org.liamjd.cantilever.models.*
import org.liamjd.cantilever.models.sqs.MarkdownUploadMsg
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

class StructureManager {

    private val structureKey = "structure.json"

    context(LambdaLogger)
    fun updateStructure(
        context: Context,
        s3Client: S3Client,
        sourceBucket: String,
        workingBucket: String,
        markdown: MarkdownUploadMsg,
        srcKey: String,
    ) {
        info("Looking for and updating $structureKey from $workingBucket")
        // Fetch the template file from the source bucket, or abort
        val templateKey = "templates/" + markdown.metadata.template + ".html.hbs"
        val template = try {
            val obj = s3Client.getObject(GetObjectRequest.builder().key(templateKey).bucket(sourceBucket).build())
            val lastModified = obj.response().lastModified().toLocalDateTime()
            Template(templateKey, lastModified)
        } catch (nske: NoSuchKeyException) {
            log("Cannot find template file $templateKey; aborting")
            return
        }

        // construct the Post object from markdown file
        val post = Post(
            title = markdown.metadata.title,
            srcKey = srcKey,
            url = markdown.metadata.slug,
            template = template,
            lastUpdated = markdown.metadata.lastModified
        )

        // NOPE - IF THE STRUCTURE DOESN'T EXIST, call a new Lambda to build it
        // IT'S NOT IMMEDIATELY NEEDED AT THIS STAGE. I THINK.

        // look for existing structure file
        val structure = if (!s3Client.objectExists(structureKey, workingBucket)) {
            error("Structure file does not exist; creating it from template: $template and post: $post")
            val layouts = Layouts(listOf(template))
            val posts = mapOf(srcKey to post)
            val items = Items(posts)
            Structure(layouts, items)
        } else {
            val json = String(
                s3Client.getObjectAsBytes(GetObjectRequest.builder().key(structureKey).bucket(workingBucket).build())
                    .asByteArray()
            )
            info("Loading existing structure from json file $structureKey")
            loadStructureFromFile(json)
        }

        val structureToSave = Json.encodeToString(Structure.serializer(), structure)
        info("Saving updated structure json file")
        val putRequest = PutObjectRequest.builder().key(structureKey).contentLength(structureToSave.length.toLong())
            .bucket(workingBucket).build()
        s3Client.putObject(putRequest, RequestBody.fromBytes(structureToSave.toByteArray()))

    }

    private fun loadStructureFromFile(json: String): Structure {
        return Json.decodeFromString<Structure>(json)
    }
}

/**
 * Utility method to check if a given object exists
 * TODO move this to common shared project
 */
fun S3Client.objectExists(key: String, bucket: String): Boolean {
    println("Checking to see if $key exists in bucket $bucket")
    var exists = true
    try {
        val headResponse: HeadObjectResponse = this
            .headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
    } catch (e: NoSuchKeyException) {
        exists = false
    } catch (se: S3Exception) {
        exists = false
    }
    return exists
}
