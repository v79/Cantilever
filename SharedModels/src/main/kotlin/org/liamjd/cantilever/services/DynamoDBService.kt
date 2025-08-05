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
     * Delete a content node from DynamoDB
     * @param srcKey The source key for the content node
     * @param projectDomain The domain of the project
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     */
    suspend fun deleteContentNode(
        srcKey: String,
        projectDomain: String,
        contentType: SOURCE_TYPE
    )

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
     * Get the count of content nodes for a specific project domain and content type
     * @param projectDomain The project domain
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @return The count of content nodes for the specified domain and content type
     */
    suspend fun getNodeCount(
        projectDomain: String,
        contentType: SOURCE_TYPE
    ): Int

    /**
     * List all nodes for a specific project domain and content type
     * @param domain The project domain
     * @param type The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @return A list of content nodes for the specified domain and content type. These will be of type [ContentNode].
     * Recommend that you use list.filterIsInstance<ContentNode.*>() to filter the list to the specific type you need.
     */
    suspend fun listAllNodesForProject(domain: String, type: SOURCE_TYPE): List<ContentNode>

    /**
     * Get a list of content nodes with specific bespoke attributes for a project domain and content type
     * For instance; to retrieve the list of all posts with a particular bespoke attribute (say, "mood"), you can pass the mood as an attribute.
     * @param projectDomain The project domain
     * @param contentType The type of content (e.g. Pages, Posts, Templates, Statics, Images)
     * @param attributes A map of attributes to filter the content nodes
     * @return A list of the src keys of nodes that match the specified attributes
     */
    suspend fun getKeyListMatchingAttributes(
        projectDomain: String,
        contentType: SOURCE_TYPE,
        attributes: Map<String, String>
    ): List<String>
}