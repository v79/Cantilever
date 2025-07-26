package org.liamjd.cantilever.services.impl

import kotlinx.coroutines.future.await
import org.liamjd.cantilever.models.dynamodb.Project
import org.liamjd.cantilever.services.DynamoDBService
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.*

/**
 * Implementation of DynamoDBService for Project operations
 * @param region The AWS region to use
 * @param tableName The name of the DynamoDB table
 */
class DynamoDBServiceImpl(
    private val region: Region,
    private val tableName: String = "cantilever-dev-content-nodes" // TODO: This should be configurable
) : DynamoDBService {

    private val dynamoDbClient: DynamoDbAsyncClient by lazy {
        DynamoDbAsyncClient.builder()
            .region(region)
            .build()
    }

    /**
     * Get a project by its domain
     * @param domain The project domain
     * @return The project if found, null otherwise
     */
    override suspend fun getProject(domain: String): Project? {
        val key = mapOf(
            "domain#type" to AttributeValue.builder().s("$domain#project").build(),
            "srcKey" to AttributeValue.builder().s("$domain.yaml").build()
        )

        val request = GetItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build()

        val response = dynamoDbClient.getItem(request).await()

        return if (response.hasItem()) {
            mapToProject(response.item())
        } else {
            null
        }
    }

    /**
     * Save a project to DynamoDB
     * @param project The project to save
     * @return The saved project
     */
    override suspend fun saveProject(project: Project): Project {
        val item = mapOf(
            "domain#type" to AttributeValue.builder().s("${project.domain}#project").build(),
            "srcKey" to AttributeValue.builder().s(project.srcKey).build(),
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

        dynamoDbClient.putItem(request).await()

        return project
    }

    /**
     * Delete a project from DynamoDB
     * @param domain The project domain
     * @param projectName The project name
     * @return true if the project was deleted, false otherwise
     */
    override suspend fun deleteProject(domain: String, projectName: String): Boolean {
        val key = mapOf(
            "domain#type" to AttributeValue.builder().s("$domain#project").build(),
            "srcKey" to AttributeValue.builder().s("$domain/$domain.yaml").build()
        )

        val request = DeleteItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build()

        val response = dynamoDbClient.deleteItem(request).await()

        return response.sdkHttpResponse().isSuccessful
    }

    /**
     * List all projects for a domain
     * @param domain The project domain
     * @return A list of projects for the domain
     */
    override suspend fun listProjects(domain: String): List<Project> {
        val request = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("domain#type = :domainType")
            .expressionAttributeValues(
                mapOf(":domainType" to AttributeValue.builder().s("$domain#project").build())
            )
            .build()

        val response = dynamoDbClient.query(request).await()

        return response.items().map { item -> mapToProject(item) }
    }

    /**
     * List all projects
     * @return A list of all projects
     */
    override suspend fun listAllProjects(): List<Project> {
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

        val response = dynamoDbClient.scan(request).await()

        return response.items().map { item -> mapToProject(item) }
    }

    /**
     * Map a DynamoDB item to a Project
     * @param item The DynamoDB item
     * @return The Project
     */
    private fun mapToProject(item: Map<String, AttributeValue>): Project {
        return Project(
            domain = item["domain"]?.s() ?: "",
            projectName = item["projectName"]?.s() ?: "",
            author = item["author"]?.s() ?: "",
            dateFormat = item["dateFormat"]?.s() ?: "",
            dateTimeFormat = item["dateTimeFormat"]?.s() ?: ""
        )
    }
}