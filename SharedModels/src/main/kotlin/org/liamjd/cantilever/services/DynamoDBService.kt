package org.liamjd.cantilever.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentNode

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
    ): DynamoDBResult

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
        attributes: Map<String, String>,
    ): List<String>

    /**
     * Get a list of content nodes with specific bespoke attributes for a project domain and content type,
     * For instance; to retrieve the list of all posts with a particular bespoke attribute (say, "mood"), you can pass the mood as an attribute.
     * The results will be limited to the specified number and ordered by the specified attribute.
     * @param projectDomain The project domain
     * @param contentType The type of content (e.g. Pages, Posts, Templates, Statics, Images)
     * @param attributes A map of attributes to filter the content nodes
     * @param limit The maximum number of results to return
     * @param descending Whether to order the results in descending order (default is true)
     * @return A list of the src keys of nodes that match the specified attributes, limited to the specified number and ordered by the specified attribute.
     * If no results are found, an empty list is returned.
     */
    suspend fun getKeyListMatchingAttributes(
        projectDomain: String,
        contentType: SOURCE_TYPE,
        attributes: Map<String, String>,
        limit: Int,
        descending: Boolean = true
    ): List<String>

    /**
     * Get a list of content nodes that match a specific template key for a project domain and content type.
     * This is useful for retrieving all nodes that use a specific template.
     * @param projectDomain The project domain
     * @param contentType The type of content (e.g., Pages or Posts; not applicable for others)
     * @param templateKey The key of the template to match,
     * @return A list of src keys of nodes that match the specified template key
     */
    suspend fun getKeyListMatchingTemplate(
        projectDomain: String,
        contentType: SOURCE_TYPE,
        templateKey: String
    ): List<String>

    /**
     * Get a list of keys from a Local Secondary Index (LSI) for a specific project domain and content type.
     * This is useful for retrieving keys based on attributes defined in the LSI.
     * @param projectDomain The project domain
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @param lsiName The name of the Local Secondary Index to query
     * @param attribute A pair containing the attribute key and value to filter the results
     *                 (e.g., Pair("mood", "happy") to filter by mood)
     * @param operation The operation to perform on the attribute (default is "=")
     * @param limit The maximum number of results to return (default is 100)
     * @param descending Whether to order the results in descending order (default is true)
     * @return A list of src keys that match the specified attributes in the LSI
     */
    suspend fun getKeyListFromLSI(
        projectDomain: String,
        contentType: SOURCE_TYPE,
        lsiName: String,
        attribute: Pair<String, String>,
        operation: String = "=",
        limit: Int = 100,
        descending: Boolean = true
    ): List<String>
}