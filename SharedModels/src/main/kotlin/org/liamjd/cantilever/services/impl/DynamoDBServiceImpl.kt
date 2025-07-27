package org.liamjd.cantilever.services.impl

import kotlinx.coroutines.future.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.TemplateListDTO
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
    private val enableLogging: Boolean = true,
    private val dynamoDbClient: DynamoDbAsyncClient
) : DynamoDBService {

    /**
     * Log a message with the specified level
     * @param level The log level (INFO, WARN, ERROR)
     * @param message The message to log
     */
    private fun log(level: String, message: String) {
        if (enableLogging) {
            println("[$level] DynamoDBService: $message")
        }
    }

    /**
     * Log an exception with the specified level
     * @param level The log level (INFO, WARN, ERROR)
     * @param message The message to log
     * @param e The exception to log
     */
    private fun log(level: String, message: String, e: Throwable) {
        if (enableLogging) {
            println("[$level] DynamoDBService: $message")
            println("[$level] Exception: ${e.javaClass.simpleName}: ${e.message}")
            e.stackTrace.take(5).forEach { println("[$level]   at $it") }
        }
    }

    /**
     * Get a project by its domain
     * @param domain The project domain
     * @return The project if found, null otherwise
     */
    override suspend fun getProject(domain: String): CantileverProject? {
        log("INFO", "Getting project with domain: $domain")

        try {
            val key = mapOf(
                "domain#type" to AttributeValue.builder().s("$domain#project").build(),
                "srcKey" to AttributeValue.builder().s("$domain.yaml").build()
            )

            val request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()

            log("INFO", "Executing GetItem request for domain: $domain")
            val response = dynamoDbClient.getItem(request).await()

            return if (response.hasItem()) {
                log("INFO", "Project found for domain: $domain")
                mapToProject(response.item())
            } else {
                log("INFO", "No project found for domain: $domain")
                null
            }
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to get project with domain: $domain", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while getting project with domain: $domain", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while getting project with domain: $domain", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while getting project with domain: $domain", e)
            throw e
        }
    }

    /**
     * Save a project to DynamoDB
     * @param project The project to save
     * @return The saved project
     */
    override suspend fun saveProject(project: CantileverProject): CantileverProject {
        log("INFO", "Saving project: ${project.projectName} for domain: ${project.domain}")

        try {
            val item = mapOf(
                "domain#type" to AttributeValue.builder().s("${project.domain}#project").build(),
                "srcKey" to AttributeValue.builder().s(project.projectKey).build(),
                "domain" to AttributeValue.builder().s(project.domain).build(),
                "projectName" to AttributeValue.builder().s(project.projectName).build(),
                "author" to AttributeValue.builder().s(project.author).build(),
                "dateFormat" to AttributeValue.builder().s(project.dateFormat).build(),
                "dateTimeFormat" to AttributeValue.builder().s(project.dateTimeFormat).build(),
                "type#lastUpdated" to AttributeValue.builder().s("project#${System.currentTimeMillis()}").build()
            )

            val request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build()

            log("INFO", "Executing PutItem request for project: ${project.projectName}")
            dynamoDbClient.putItem(request).await()

            log("INFO", "Successfully saved project: ${project.projectName} for domain: ${project.domain}")
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
        log("INFO", "Deleting project: $projectName for domain: $domain")

        try {
            val key = mapOf(
                "domain#type" to AttributeValue.builder().s("$domain#project").build(),
                "srcKey" to AttributeValue.builder().s("$domain.yaml").build()
            )

            val request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()

            log("INFO", "Executing DeleteItem request for project: $projectName")
            val response = dynamoDbClient.deleteItem(request).await()

            val isSuccessful = response.sdkHttpResponse().isSuccessful
            if (isSuccessful) {
                log("INFO", "Successfully deleted project: $projectName for domain: $domain")
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
     * List all projects for a domain
     * @param domain The project domain
     * @return A list of projects for the domain
     */
    override suspend fun listProjects(domain: String): List<CantileverProject> {
        log("INFO", "Listing projects for domain: $domain")

        try {
            val request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("domain#type = :domainType")
                .expressionAttributeValues(
                    mapOf(":domainType" to AttributeValue.builder().s("$domain#project").build())
                )
                .build()

            log("INFO", "Executing Query request for domain: $domain")
            val response = dynamoDbClient.query(request).await()

            val projects = response.items().map { item -> mapToProject(item) }
            log("INFO", "Found ${projects.size} projects for domain: $domain")

            return projects
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to list projects for domain: $domain", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while listing projects for domain: $domain", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while listing projects for domain: $domain", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while listing projects for domain: $domain", e)
            throw e
        }
    }

    /**
     * List all projects
     * @return A list of all projects
     */
    override suspend fun listAllProjects(): List<CantileverProject> {
        log("INFO", "Listing all projects")

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

            log("INFO", "Executing Scan request for all projects")
            val response = dynamoDbClient.scan(request).await()

            val projects = response.items().map { item -> mapToProject(item) }
            log("INFO", "Found ${projects.size} projects in total")

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
     * @param attributes A map of attributes for the content node // TODO: Support more complex attributes
     */
    override suspend fun upsertContentNode(
        srcKey: String,
        projectDomain: String,
        contentType: SOURCE_TYPE,
        attributes: Map<String, String>
    ): Boolean {
        log("INFO", "Upserting content node: $srcKey in domain: $projectDomain of type: ${contentType.dbType}")

        try {
            val item = mutableMapOf<String, AttributeValue>(
                "domain#type" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build(),
                "srcKey" to AttributeValue.builder().s(srcKey).build(),
                "type#lastUpdated" to AttributeValue.builder().s("${contentType.dbType}#${System.currentTimeMillis()}")
                    .build()
            )

            // Add attributes to the item
            attributes.forEach { (key, value) ->
                item[key] = AttributeValue.builder().s(value).build() // might not always be a string
            }

            val request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build()

            log("INFO", "Executing PutItem request for content node: $srcKey")
            val response = dynamoDbClient.putItem(request).await()

            if (response.sdkHttpResponse().isSuccessful) {
                log("INFO", "Successfully upserted content node: $srcKey in domain: $projectDomain")
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
        return false
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
        log("INFO", "Getting content node: $srcKey in domain: $projectDomain of type: ${contentType.dbType}")

        try {
            val key = mapOf(
                "domain#type" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build(),
                "srcKey" to AttributeValue.builder().s(srcKey).build()
            )

            val request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()

            log("INFO", "Executing GetItem request for content node: $srcKey")
            val response = dynamoDbClient.getItem(request).await()

            return if (response.hasItem()) {
                log("INFO", "Content node found: $srcKey")
                when (contentType) {
                    SOURCE_TYPE.Templates -> mapToTemplateNode(response.item())
                    else -> throw IllegalArgumentException("Unsupported content type: ${contentType.dbType}")
                }
            } else {
                log("INFO", "No content node found for srcKey: $srcKey")
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
     * List all templates for a specific domain
     * @param domain The project domain
     * @return A list of templates for the domain
     */
    override suspend fun listAllTemplates(domain: String): TemplateListDTO {
        log("INFO", "Listing all templates for domain: $domain")

        try {
            val request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("#pk = :domainType")
                .expressionAttributeNames(
                    mapOf("#pk" to "domain#type")
                )
                .expressionAttributeValues(
                    mapOf(":domainType" to AttributeValue.builder().s("$domain#template").build())
                )
                .build()

            log("INFO", "Executing Query request for templates in domain: $domain")
            val response = dynamoDbClient.query(request).await()

            return if (response.count() > 0) {
                log("INFO", "Found ${response.count()} templates for domain: $domain")
                response.items().map { item -> mapToTemplateNode(item) }.let { templates ->
                    TemplateListDTO(
                        templates = templates,
                        count = response.count(),
                        lastUpdated = Clock.System.now()
                    )
                }
            } else {
                log("INFO", "No templates found for domain: $domain")
                TemplateListDTO(
                    templates = emptyList(),
                    count = 0,
                    lastUpdated = Clock.System.now()
                )
            }
        } catch (e: DynamoDbException) {
            log("ERROR", "Failed to list templates for domain: $domain", e)
            throw e
        } catch (e: AwsServiceException) {
            log("ERROR", "AWS service error while listing templates for domain: $domain", e)
            throw e
        } catch (e: SdkClientException) {
            log("ERROR", "SDK client error while listing templates for domain: $domain", e)
            throw e
        } catch (e: Exception) {
            log("ERROR", "Unexpected error while listing templates for domain: $domain", e)
            throw e
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

            val lastUpdatedStr = item["type#lastUpdated"]?.s()
            val lastUpdated = try {
                if (lastUpdatedStr != null && lastUpdatedStr.contains("#")) {
                    val timestamp = lastUpdatedStr.split("#")[1].toLongOrNull() ?: 0L
                    Instant.fromEpochSeconds(timestamp)
                } else {
                    log("WARN", "Invalid type#lastUpdated format in template item: $lastUpdatedStr")
                    Clock.System.now()
                }
            } catch (e: Exception) {
                log("WARN", "Failed to parse lastUpdated timestamp: $lastUpdatedStr", e)
                Clock.System.now()
            }

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
}