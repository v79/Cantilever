package org.liamjd.cantilever.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.TemplateListDTO

/**
 * Interface for DynamoDB operations related to Cantilever projects
 */
interface DynamoDBService {

    var logger: LambdaLogger?

    /**
     * Get a project by its domain
     * @param domain The project domain
     * @return The project if found, null otherwise
     */
    suspend fun getProject(domain: String): CantileverProject?

    /**
     * Save a project to DynamoDB
     * @param project The project to save
     * @return The saved project
     */
    suspend fun saveProject(project: CantileverProject): CantileverProject

    /**
     * Delete a project from DynamoDB
     * @param domain The project domain
     * @param projectName The project name
     * @return true if the project was deleted, false otherwise
     */
    suspend fun deleteProject(domain: String, projectName: String): Boolean

    /**
     * List all projects for a domain
     * @param domain The project domain
     * @return A list of projects for the domain
     */
    suspend fun listProjects(domain: String): List<CantileverProject>

    /**
     * List all projects
     * @return A list of all projects
     */
    suspend fun listAllProjects(): List<CantileverProject>

    /**
     * Upsert a content node in DynamoDB. This will either insert a new content node or update an existing one.
     * The content node is identified by its source key, project domain and content type.
     * @param srcKey The source key for the content node
     * @param projectDomain The domain of the project
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @param node The content node to upsert
     * @param attributes A map of additional attributes for the content node // TODO: Support other value types
     * @return true if the content node was successfully upserted, false otherwise
     */
    suspend fun upsertContentNode(
        srcKey: String,
        projectDomain: String,
        contentType: SOURCE_TYPE,
        node: ContentNode,
        attributes: Map<String, String>
    ): Boolean

    /**
     * Get a content node by its source key, project domain and content type
     * @param srcKey The source key for the content node
     * @param projectDomain The domain of the project
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @return The content node if found, null otherwise
     */
    suspend fun getContentNode(
        srcKey: String,
        projectDomain: String,
        contentType: SOURCE_TYPE
    ): ContentNode?

    /**
     * List all templates for a specific domain
     * @param domain The project domain
     * @return A list of templates for the domain
     */
    suspend fun listAllTemplates(domain: String): TemplateListDTO
}