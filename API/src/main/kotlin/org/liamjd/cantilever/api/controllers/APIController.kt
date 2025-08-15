package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service

abstract class APIController(val sourceBucket: String, val generationBucket: String) : KoinComponent {

    val s3Service: S3Service by inject()
    val dynamoDBService: DynamoDBService by inject()
    val contentTree: ContentTree = ContentTree()
    lateinit var project: CantileverProject

    /**
     * Load the content tree from the S3 bucket
     */
    @Deprecated("Replace with DynamoDBService calls")
    fun loadContentTree(domain: String): Boolean {
        val metadataKey = "$domain/${S3_KEY.metadataKey}"
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
            error("Error reading '$metadataKey' from bucket $generationBucket: ${e.message}")
            return false
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