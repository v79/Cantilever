package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.models.Layouts
import org.liamjd.cantilever.models.Post
import org.liamjd.cantilever.models.Structure
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*

@Deprecated(message = "Should be handled in the StructureController now")
class StructureManager {

    private val structureKey = "generated/structure.json"
    private val templatesKey = "templates/"

    context(LambdaLogger)
    fun updateStructure(
        context: Context,
        s3Client: S3Client,
        sourceBucket: String,
        markdown: SqsMsgBody.MarkdownPostUploadMsg,
        srcKey: String,
    ) {
        info("Looking for and updating $structureKey from $sourceBucket")
        // Fetch the template file from the source bucket, or abort
        val templateKey = templatesKey + markdown.metadata.template + ".html.hbs"
        val template = try {
            val obj = s3Client.getObject(GetObjectRequest.builder().key(templateKey).bucket(sourceBucket).build())
            val lastModified = obj.response().lastModified()
            Template(templateKey, lastModified.toKotlinInstant())
        } catch (nske: NoSuchKeyException) {
            log("Cannot find template file $templateKey; aborting")
            return
        }

        // construct the Post object from markdown file
        val post = Post(
            title = markdown.metadata.title,
            srcKey = srcKey,
            url = markdown.metadata.slug,
            templateKey = template.key,
            date = markdown.metadata.date,
            lastUpdated = markdown.metadata.lastModified
        )

        // NOPE - IF THE STRUCTURE DOESN'T EXIST, call a new Lambda to build it
        // IT'S NOT IMMEDIATELY NEEDED AT THIS STAGE. I THINK.

        // look for existing structure file
        val structure = if (!s3Client.objectExists(structureKey, sourceBucket)) {
            error("Structure file does not exist; creating it from template: $template and post: $post")
            val layouts = Layouts(mutableMapOf(templateKey to template))
            Structure(layouts, mutableListOf(), 0)
        } else {
            info("Loading existing structure from json file $structureKey")
            val json = String(
                s3Client.getObjectAsBytes(GetObjectRequest.builder().key(structureKey).bucket(sourceBucket).build())
                    .asByteArray()
            )
            loadStructureFromFile(json)
        }
        info("Adding post $srcKey, template ${template.key} to Structure (had ${structure.posts.size} posts)")
        structure.posts.add(post)
        structure.layouts.templates[templateKey] = template


        val structureToSave = Json.encodeToString(Structure.serializer(), structure)
        info("Saving updated structure json file (${structureToSave.length} bytes)")
        val putRequest = PutObjectRequest.builder().key(structureKey).contentLength(structureToSave.length.toLong())
            .bucket(sourceBucket).build()
        s3Client.putObject(putRequest, RequestBody.fromBytes(structureToSave.toByteArray()))

    }

    private fun loadStructureFromFile(json: String): Structure {
        return Json.decodeFromString(json)
    }
}

/**
 * Utility method to check if a given object exists by simply calling the Head request and returning false if there is an exception
 * TODO move this to common shared project
 */
fun S3Client.objectExists(key: String, bucket: String): Boolean {
    var exists = true
    try {
        this
            .headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
    } catch (e: NoSuchKeyException) {
        exists = false
    } catch (se: S3Exception) {
        exists = false
    }
    println("Checking to see if $key exists in bucket $bucket: $exists")
    return exists
}
