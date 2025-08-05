package org.liamjd.cantilever.services.impl

import com.amazonaws.services.lambda.runtime.LambdaLogger
import kotlinx.coroutines.future.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.TemplateListDTO
import org.liamjd.cantilever.services.AWSLogger
import org.liamjd.cantilever.services.DynamoDBService
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.*

/**
 * Implementation of DynamoDBService for Project operations
 * @param region The AWS region to use
 * @param tableName The name of the DynamoDB table
 * @param enableLogging Whether to enable logging (default: true)
 */
class DynamoDBServiceImpl(
    private val region: Region,
    val tableName: String = "cantilever-dev-content-nodes", // TODO: This should be configurable
    private val dynamoDbClient: DynamoDbAsyncClient,
    enableLogging: Boolean = true,
) : DynamoDBService, AWSLogger(enableLogging, "DynamoDBService") {

    override var logger: LambdaLogger? = null

    /**
     * Execute a DynamoDB operation with standard error handling
     * @param operationDescription A description of the operation (for logging)
     * @param contextInfo Additional context information for error messages
     * @param operation The operation to execute
     * @return The result of the operation
     */
    private suspend fun <T> executeDynamoOperation(
        operationDescription: String,
        contextInfo: String,
        operation: suspend () -> T
    ): T {
        log("$operationDescription: $contextInfo")

        try {
            return operation()
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to $operationDescription: $contextInfo", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while $operationDescription: $contextInfo", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while $operationDescription: $contextInfo", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while $operationDescription: $contextInfo", e)
            throw e
        }
    }

    /**
     * Get a project by its domain
     * @param domain The project domain
     * @return The project if found, null otherwise
     */
    override suspend fun getProject(domain: String): CantileverProject? {
        return executeDynamoOperation("get project", "domain: $domain") {
            val key = mapOf(
                "domain#type" to AttributeValue.builder().s("$domain#project").build(),
                "srcKey" to AttributeValue.builder().s("$domain.yaml").build()
            )

            val request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()

            log("Executing GetItem request for domain: $domain")
            val response = dynamoDbClient.getItem(request).await()

            if (response.hasItem()) {
                log("Project found for domain: $domain")
                mapToProject(response.item())
            } else {
                log("No project found for domain: $domain")
                null
            }
        }
    }

    /**
     * Save a project to DynamoDB
     * @param project The project to save
     * @return The saved project
     */
    override suspend fun saveProject(project: CantileverProject): CantileverProject {
        log("Saving project: ${project.projectName} for domain: ${project.domain}")

        try {
            val item = mapOf(
                "domain#type" to AttributeValue.builder().s("${project.domain}#project").build(),
                "srcKey" to AttributeValue.builder().s(project.projectKey).build(),
                "domain" to AttributeValue.builder().s(project.domain).build(),
                "projectName" to AttributeValue.builder().s(project.projectName).build(),
                "author" to AttributeValue.builder().s(project.author).build(),
                "dateFormat" to AttributeValue.builder().s(project.dateFormat).build(),
                "dateTimeFormat" to AttributeValue.builder().s(project.dateTimeFormat).build(),
                "type#lastUpdated" to createLastUpdatedAttribute("project")
            )

            val request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build()

            log("Executing PutItem request for project: ${project.projectName}")
            dynamoDbClient.putItem(request).await()

            log("Successfully saved project: ${project.projectName} for domain: ${project.domain}")
            return project
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to save project: ${project.projectName} for domain: ${project.domain}", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while saving project: ${project.projectName}", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while saving project: ${project.projectName}", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while saving project: ${project.projectName}", e)
            throw e
        }
    }

    /**
     * Delete a project from DynamoDB
     * @param domain The project domain
     * @param projectName The project name
     * @return true if the project was deleted, false otherwise
     */
    override suspend fun deleteProject(domain: String, projectName: String): Boolean {
        log("Deleting project: $projectName for domain: $domain")

        try {
            val key = mapOf(
                "domain#type" to AttributeValue.builder().s("$domain#project").build(),
                "srcKey" to AttributeValue.builder().s("$domain.yaml").build()
            )

            val request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()

            log("Executing DeleteItem request for project: $projectName")
            val response = dynamoDbClient.deleteItem(request).await()

            val isSuccessful = response.sdkHttpResponse().isSuccessful
            if (isSuccessful) {
                log("Successfully deleted project: $projectName for domain: $domain")
            } else {
                log(
                    "WARN",
                    "Failed to delete project: $projectName for domain: $domain. HTTP status: ${
                        response.sdkHttpResponse().statusCode()
                    }"
                )
            }

            return isSuccessful
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to delete project: $projectName for domain: $domain", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while deleting project: $projectName", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while deleting project: $projectName", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while deleting project: $projectName", e)
            throw e
        }
    }

    /**
     * List all projects
     * @return A list of all projects
     */
    override suspend fun listAllProjects(): List<CantileverProject> {
        log("Listing all projects")

        try {
            val request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("contains(#domainType, :projectType)")
                .expressionAttributeNames(
                    mapOf("#domainType" to "domain#type")
                )
                .expressionAttributeValues(
                    mapOf(":projectType" to AttributeValue.builder().s("#project").build())
                )
                .build()

            log("Executing Scan request for all projects")
            val response = dynamoDbClient.scan(request).await()

            val projects = response.items().map { item -> mapToProject(item) }
            log("Found ${projects.size} projects in total")

            return projects
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to list all projects", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while listing all projects", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while listing all projects", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while listing all projects", e)
            throw e
        }
    }

    /**
     * Upsert a content node in DynamoDB. This will either insert a new content node or update an existing one.
     * The content node is identified by its source key, project domain and content type.
     * @param srcKey The source key for the content node
     * @param projectDomain The domain of the project
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @param node The content node to upsert
     * @param attributes A map of additional attributes for the content node
     * @return true if the content node was successfully upserted, false otherwise
     */
    override suspend fun upsertContentNode(
        srcKey: String,
        projectDomain: String,
        contentType: SOURCE_TYPE,
        node: ContentNode,
        attributes: Map<String, String>
    ): Boolean {
        log("Upserting content node: $srcKey in domain: $projectDomain of type: ${contentType.dbType}")

        try {
            val item = mutableMapOf<String, AttributeValue>(
                "domain#type" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build(),
                "srcKey" to AttributeValue.builder().s(srcKey).build(),
                "type#lastUpdated" to createLastUpdatedAttribute(contentType.dbType)
            )

            // Add node properties based on its type
            when (node) {
                is ContentNode.PostNode -> {
                    item["title"] = AttributeValue.builder().s(node.title).build()
                    item["templateKey"] = AttributeValue.builder().s(node.templateKey).build()
                    item["slug"] = AttributeValue.builder().s(node.slug).build()
                    item["date"] = AttributeValue.builder().s(node.date.toString()).build()

                    // Add the node's own attributes with attr# prefix
                    node.attributes.forEach { (key, value) ->
                        item["attr#$key"] = AttributeValue.builder().s(value).build()
                    }
                }

                is ContentNode.TemplateNode -> {
                    item["title"] = AttributeValue.builder().s(node.title).build()
                    item["sections"] = AttributeValue.builder().s(node.sections.joinToString(",")).build()
                }

                is ContentNode.StaticNode, is ContentNode.ImageNode -> {
                    // Static nodes might not have additional properties, but we can add a lastUpdated timestamp
                    item["type#lastUpdated"] = createLastUpdatedAttribute("static")
                }

                is ContentNode.PageNode, is ContentNode.FolderNode -> {
                    // These types are not fully implemented, yet
                    // Folders won't have attributes on creation but could have them later
                    log("Node type ${node.javaClass.simpleName} not fully implemented for upsert")
                }
            }

            log("Adding additional attributes: $attributes")
            // Add additional attributes to the item with attr# prefix
            attributes.forEach { (key, value) ->
                item["attr#$key"] = AttributeValue.builder().s(value).build() // might not always be a string
            }

            val request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build()

            log("Executing PutItem request for content node: $srcKey")
            val response = dynamoDbClient.putItem(request).await()

            if (response.sdkHttpResponse().isSuccessful) {
                log("Successfully upserted content node: $srcKey in domain: $projectDomain")
                return true
            } else {
                log(
                    "WARN",
                    "Received non-successful response when upserting content node: $srcKey. Status: ${
                        response.sdkHttpResponse().statusCode()
                    }"
                )
                return false
            }
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to upsert content node: $srcKey in domain: $projectDomain", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while upserting content node: $srcKey", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while upserting content node: $srcKey", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while upserting content node: $srcKey", e)
            throw e
        }
    }

    /**
     * Delete a content node from DynamoDB
     * @param srcKey The source key for the content node
     * @param projectDomain The domain of the project
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     */
    override suspend fun deleteContentNode(
        srcKey: String,
        projectDomain: String,
        contentType: SOURCE_TYPE
    ) {
        log("Deleting content node: $srcKey in domain: $projectDomain of type: ${contentType.dbType}")

        try {
            val key = mapOf(
                "domain#type" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build(),
                "srcKey" to AttributeValue.builder().s(srcKey).build()
            )

            val request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()

            log("Executing DeleteItem request for content node: $srcKey")
            val response = dynamoDbClient.deleteItem(request).await()
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to delete content node: $srcKey in domain: $projectDomain", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while deleting content node: $srcKey", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while deleting content node: $srcKey", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while deleting content node: $srcKey", e)
            throw e
        }
    }

    /**
     * Get a content node by its source key, project domain and content type
     * @param srcKey The source key for the content node
     * @param projectDomain The domain of the project
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @return The content node if found, null otherwise
     */
    override suspend fun getContentNode(
        srcKey: String,
        projectDomain: String,
        contentType: SOURCE_TYPE
    ): ContentNode? {
        log("Getting content node: $srcKey in domain: $projectDomain of type: ${contentType.dbType}")

        try {
            val key = mapOf(
                "domain#type" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build(),
                "srcKey" to AttributeValue.builder().s(srcKey).build()
            )

            val request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()

            log("Executing GetItem request for content node: $srcKey")
            val response = dynamoDbClient.getItem(request).await()

            return if (response.hasItem()) {
                log("Content node found: $srcKey")
                when (contentType) {
                    SOURCE_TYPE.Templates -> mapToTemplateNode(response.item())
                    SOURCE_TYPE.Posts -> mapToPostNode(response.item())
                    SOURCE_TYPE.Statics -> mapToStaticNode(response.item())
                    else -> throw IllegalArgumentException("Unsupported content type: ${contentType.dbType}")
                }
            } else {
                log("No content node found for srcKey: $srcKey")
                null
            }
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to get content node: $srcKey in domain: $projectDomain", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while getting content node: $srcKey", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while getting content node: $srcKey", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while getting content node: $srcKey", e)
            throw e
        }
    }


    /**
     * Get the count of content nodes for a specific project domain and content type
     * @param projectDomain The project domain
     * @param contentType The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @return The count of content nodes for the specified domain and content type
     */
    override suspend fun getNodeCount(
        projectDomain: String,
        contentType: SOURCE_TYPE
    ): Int {
        log("Getting node count for domain: $projectDomain of type: ${contentType.dbType}")

        return executeDynamoOperation(
            operationDescription = "get node count",
            contextInfo = "domain: $projectDomain, type: ${contentType.dbType}"
        ) {
            val request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("#domainType = :domainTypeValue")
                .expressionAttributeNames(mapOf("#domainType" to "domain#type"))
                .expressionAttributeValues(
                    mapOf(
                        ":domainTypeValue" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build()
                    )
                )
                .select(Select.COUNT) // Only get the count
                .build()

            log("Executing Query request for node count in domain: $projectDomain")
            val response = dynamoDbClient.query(request).await()

            log("Node count for domain: $projectDomain is ${response.count()}")
            response.count()
        }
    }

    /**
     * List all nodes for a specific project domain and content type
     * @param domain The project domain
     * @param type The type of content (e.g., Pages, Posts, Templates, Statics, Images)
     * @return A list of content nodes for the specified domain and content type
     */
    override suspend fun listAllNodesForProject(
        domain: String,
        type: SOURCE_TYPE
    ): List<ContentNode> {
        log("Listing all nodes for domain: $domain of type: ${type.dbType}")

        return executeDynamoOperation(
            operationDescription = "list all nodes for domain: $domain",
            contextInfo = "domain: $domain, type: ${type.dbType}"
        ) {
            val request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("#pk = :domainType")
                .expressionAttributeNames(
                    mapOf("#pk" to "domain#type")
                )
                .expressionAttributeValues(
                    mapOf(":domainType" to AttributeValue.builder().s("$domain#${type.dbType}").build())
                )
                .build()

            val response = dynamoDbClient.query(request).await()

            if (response.count() > 0) {
                log("Found ${response.count()} nodes for domain: $domain of type: ${type.dbType}")
                response.items().map { item ->
                    when (type) {
                        SOURCE_TYPE.Posts -> mapToPostNode(item)
                        SOURCE_TYPE.Templates -> mapToTemplateNode(item)
                        SOURCE_TYPE.Statics -> mapToStaticNode(item)
                        else -> throw IllegalArgumentException("Unsupported content type: ${type.dbType}")
                    }
                }
            } else {
                log("No nodes found for domain: $domain of type: ${type.dbType}")
                emptyList()
            }
        }
    }

    /**
     * Map a DynamoDB item to a CantileverProject
     * @param item The DynamoDB item
     * @return The CantileverProject
     */
    private fun mapToProject(item: Map<String, AttributeValue>): CantileverProject {
        try {
            val domain = item["domain"]?.s() ?: ""
            if (domain.isEmpty()) {
                log("WARN", "Missing domain in project item: $item")
            }

            val projectName = item["projectName"]?.s() ?: ""
            if (projectName.isEmpty()) {
                log("WARN", "Missing projectName in project item for domain: $domain")
            }

            return CantileverProject(
                domain = domain,
                projectName = projectName,
                author = item["author"]?.s() ?: "",
                dateFormat = item["dateFormat"]?.s() ?: "",
                dateTimeFormat = item["dateTimeFormat"]?.s() ?: ""
            )
        } catch (e: Exception) {
            log("ERROR", "Failed to map DynamoDB item to CantileverProject: $item", e)
            throw e
        }
    }

    /**
     * Map a DynamoDB item to a ContentNode.PostNode
     * @param item The DynamoDB item
     * @return The ContentNode.PostNode
     */
    private fun mapToPostNode(item: Map<String, AttributeValue>): ContentNode.PostNode {
        try {
            val srcKey = item["srcKey"]?.s() ?: ""
            if (srcKey.isEmpty()) {
                log("WARN", "Missing srcKey in post item: $item")
            }

            val lastUpdated = extractLastUpdatedTimestamp(item, "post")

            // Parse the date string directly as a LocalDate instead of going through Instant
            val dateStr = item["date"]?.s() ?: ""
            val postDate = if (dateStr.isNotEmpty()) {
                try {
                    kotlinx.datetime.LocalDate.parse(dateStr)
                } catch (e: Exception) {
                    log("WARN", "Failed to parse date: $dateStr, using current date", e)
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                }
            } else {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            }

            return ContentNode.PostNode(
                srcKey = srcKey,
                title = item["title"]?.s() ?: "",
                templateKey = item["templateKey"]?.s() ?: "",
                date = postDate,
                slug = item["slug"]?.s() ?: "",
                lastUpdated = lastUpdated,
                attributes = item.filter { it.key.startsWith("attr#") }
                    .mapKeys { it.key.removePrefix("attr#") }
                    .mapValues { it.value.s() ?: "" },
            )
        } catch (e: Exception) {
            log("ERROR", "Failed to map DynamoDB item to ContentNode.PostNode: $item", e)
            throw e
        }
    }

    /**
     * Map a DynamoDB item to a ContentNode.TemplateNode
     * @param item The DynamoDB item
     * @return The ContentNode.TemplateNode
     */
    private fun mapToTemplateNode(item: Map<String, AttributeValue>): ContentNode.TemplateNode {
        try {
            val srcKey = item["srcKey"]?.s() ?: ""
            if (srcKey.isEmpty()) {
                log("WARN", "Missing srcKey in template item: $item")
            }

            val lastUpdated = extractLastUpdatedTimestamp(item, "template")

            return ContentNode.TemplateNode(
                srcKey = srcKey,
                title = item["title"]?.s() ?: "",
                sections = item["sections"]?.s()?.split(",") ?: emptyList(),
                lastUpdated = lastUpdated
            )
        } catch (e: Exception) {
            log("ERROR", "Failed to map DynamoDB item to ContentNode.TemplateNode: $item", e)
            throw e
        }
    }

    /**
     * Map a DynamoDB item to a ContentNode.StaticNode
     * @param item The DynamoDB item
     * @return The ContentNode.StaticNode
     */
    private fun mapToStaticNode(item: Map<String, AttributeValue>): ContentNode.StaticNode {
        try {
            val srcKey = item["srcKey"]?.s() ?: ""
            if (srcKey.isEmpty()) {
                log("WARN", "Missing srcKey in static item: $item")
            }

            val lastUpdated = extractLastUpdatedTimestamp(item, "static")

            return ContentNode.StaticNode(
                srcKey = srcKey,
                lastUpdated = lastUpdated
            )
        } catch (e: Exception) {
            log("ERROR", "Failed to map DynamoDB item to ContentNode.StaticNode: $item", e)
            throw e
        }
    }

    /**
     * Extract the lastUpdated timestamp from a DynamoDB item
     * @param item The DynamoDB item
     * @param itemType The type of item (for logging purposes)
     * @return The lastUpdated timestamp as an Instant
     */
    private fun extractLastUpdatedTimestamp(item: Map<String, AttributeValue>, itemType: String): Instant {
        val lastUpdatedStr = item["type#lastUpdated"]?.s()
        return try {
            if (lastUpdatedStr != null && lastUpdatedStr.contains("#")) {
                val timestamp = lastUpdatedStr.split("#")[1].toLongOrNull() ?: 0L
                Instant.fromEpochSeconds(timestamp)
            } else {
                log("WARN", "Invalid type#lastUpdated format in $itemType item: $lastUpdatedStr")
                Clock.System.now()
            }
        } catch (e: Exception) {
            log("WARN", "Failed to parse lastUpdated timestamp: $lastUpdatedStr", e)
            Clock.System.now()
        }
    }

    /**
     * Create a lastUpdated attribute value
     * @param type The type of item
     * @return The lastUpdated attribute value
     */
    private fun createLastUpdatedAttribute(type: String): AttributeValue {
        return AttributeValue.builder().s("$type#${System.currentTimeMillis()}").build()
    }
}