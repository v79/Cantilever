package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.services.S3Service

abstract class APIController(val sourceBucket: String) : KoinComponent {

    val s3Service: S3Service by inject()
    val contentTree: ContentTree = ContentTree()

    /**
     * Load the content tree from the S3 bucket
     */
    fun loadContentTree() {
        if (s3Service.objectExists("generated/metadata.json", sourceBucket)) {
            info("Reading metadata.json from bucket $sourceBucket")
            contentTree.clear()
            val metadata = s3Service.getObjectAsString("generated/metadata.json", sourceBucket)
            val newTree = Json.decodeFromString(ContentTree.serializer(), metadata)
            contentTree.items.addAll(newTree.items)
            contentTree.templates.addAll(newTree.templates)
            contentTree.statics.addAll(newTree.statics)
        } else {
            warn("No metadata.json file found in bucket $sourceBucket; creating new empty tree")
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