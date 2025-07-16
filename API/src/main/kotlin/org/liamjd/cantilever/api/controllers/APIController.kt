package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.common.MimeType
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.repositories.ContentRepository
import org.liamjd.cantilever.repositories.ContentRepositoryFactory
import org.liamjd.cantilever.services.S3Service
import software.amazon.awssdk.regions.Region

abstract class APIController(val sourceBucket: String, val generationBucket: String) : KoinComponent {

    val s3Service: S3Service by inject()
    val contentTree: ContentTree = ContentTree()
    lateinit var project: CantileverProject
    
    // Get ContentRepository from Koin or create one if not available
    val contentRepository: ContentRepository by inject()

    /**
     * Load the content tree from DynamoDB or fallback to S3 bucket
     */
    fun loadContentTree(domain: String): Boolean {
        try {
            // Try to load from DynamoDB first
            info("Loading content tree for domain $domain from DynamoDB")
            val newTree = contentRepository.getContentTree(domain)
            
            // If we got data from DynamoDB, use it
            if (newTree.items.isNotEmpty() || newTree.templates.isNotEmpty() || 
                newTree.statics.isNotEmpty() || newTree.images.isNotEmpty()) {
                contentTree.clear()
                contentTree.items.addAll(newTree.items)
                contentTree.templates.addAll(newTree.templates)
                contentTree.statics.addAll(newTree.statics)
                contentTree.images.addAll(newTree.images)
                return true
            }
            
            // If DynamoDB is empty, try to load from S3 as fallback
            val metadataKey = "$domain/metadata.json"
            if (s3Service.objectExists(metadataKey, generationBucket)) {
                info("Reading $metadataKey from bucket $generationBucket (fallback)")
                contentTree.clear()
                val metadata = s3Service.getObjectAsString(metadataKey, generationBucket)
                val s3Tree = Json.decodeFromString(ContentTree.serializer(), metadata)
                contentTree.items.addAll(s3Tree.items)
                contentTree.templates.addAll(s3Tree.templates)
                contentTree.statics.addAll(s3Tree.statics)
                contentTree.images.addAll(s3Tree.images)
                
                // Migrate data from S3 to DynamoDB
                info("Migrating content tree from S3 to DynamoDB")
                saveContentTree(domain)
                
                return true
            } else {
                warn("No content tree found for domain $domain in DynamoDB or S3; please regenerate new empty tree")
                return false
            }
        } catch (e: Exception) {
            error("Error loading content tree for domain $domain: ${e.message}")
            return false
        }
    }

    /**
     * Save the content tree to DynamoDB and S3 bucket (for backward compatibility)
     */
    fun saveContentTree(domain: String) {
        try {
            // Save to DynamoDB
            info("Saving content tree for domain $domain to DynamoDB")
            val success = contentRepository.saveContentTree(domain, contentTree)
            
            if (!success) {
                warn("Failed to save content tree to DynamoDB, falling back to S3 only")
            }
            
            // Also save to S3 for backward compatibility
            val metadataKey = "$domain/metadata.json"
            info("Saving content tree $metadataKey to bucket $generationBucket (backward compatibility)")
            val json = Json { prettyPrint = true }
            val metadata = json.encodeToString(ContentTree.serializer(), contentTree)
            s3Service.putObjectAsString(metadataKey, generationBucket, metadata, MimeType.json.toString())
        } catch (e: Exception) {
            error("Error saving content tree for domain $domain: ${e.message}")
        }
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