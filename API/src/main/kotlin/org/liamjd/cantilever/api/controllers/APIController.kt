package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.common.MimeType
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.services.S3Service

abstract class APIController(val sourceBucket: String, val generationBucket: String) : KoinComponent {

    val s3Service: S3Service by inject()
    val contentTree: ContentTree = ContentTree()
    lateinit var project: CantileverProject

    /**
     * Load the content tree from the S3 bucket
     */
    fun loadContentTree(domain: String): Boolean {
        val metadataKey = "$domain/metadata.json"
        try {
            if (s3Service.objectExists(metadataKey, generationBucket)) {
                info("Reading $metadataKey from bucket $generationBucket")
                contentTree.clear()
                val metadata = s3Service.getObjectAsString(metadataKey, generationBucket)
                val newTree = Json.decodeFromString(ContentTree.serializer(), metadata)
                contentTree.items.addAll(newTree.items)
                contentTree.templates.addAll(newTree.templates)
                contentTree.statics.addAll(newTree.statics)
                contentTree.images.addAll(newTree.images)
                return true
            } else {
                warn("No '$metadataKey' file found in bucket $generationBucket; please regenerate new empty tree")
                return false
            }
        } catch (e: Exception) {
            error("Error reading $metadataKey from bucket $generationBucket: ${e.message}")
            return false
        }
    }

    /**
     * Save the content tree to the S3 bucket after a change
     */
    fun saveContentTree(domain: String) {
        val metadataKey =
            "$domain/metadata.json"
        info("Saving content tree $metadataKey to bucket $sourceBucket")
        val json = Json { prettyPrint = true }
        val metadata = json.encodeToString(ContentTree.serializer(), contentTree)
        s3Service.putObjectAsString(metadataKey, generationBucket, metadata, MimeType.json.toString())
    }

    /**
     * Load the project definition  'cantilever.yaml' from the S3 bucket
     */
    fun loadProjectDefinition(domain: String) {
        val projectKey = "$domain.yaml"
        if (s3Service.objectExists(projectKey, sourceBucket)) {
            info("Reading $projectKey from bucket $sourceBucket")
            val projectYaml = s3Service.getObjectAsString(projectKey, sourceBucket)
            project = Yaml.default.decodeFromString(CantileverProject.serializer(), projectYaml)
        } else {
            error("No '$projectKey' file found in bucket $sourceBucket!")
        }
    }

    /**
     * A slightly nicer logging mechanism than println?
     * Override these to add the class name?
     */
    open fun info(message: String) = println(message)
    open fun warn(message: String) = println("WARN: $message")
    open fun error(message: String) = println("ERROR: $message")
    open fun debug(message: String) = println("DEBUG: $message")
}