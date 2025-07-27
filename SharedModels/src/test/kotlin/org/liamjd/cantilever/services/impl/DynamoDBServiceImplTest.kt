package org.liamjd.cantilever.services.impl

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.liamjd.cantilever.models.CantileverProject
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import java.net.URI

/**
 * Basic tests for DynamoDBServiceImpl
 * Note: These tests don't actually connect to DynamoDB, they just verify the class structure
 */
class DynamoDBServiceImplTest {

    companion object {
        @JvmStatic
        private val localstack = LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.DYNAMODB)

        @BeforeAll
        @JvmStatic
        fun startContainer() {
            println("Starting LocalStack container")
            localstack.start()
        }

        @AfterAll
        @JvmStatic
        fun stopContainer() {
            println("Stopping LocalStack container")
            localstack.stop()
        }
    }

    private lateinit var service: DynamoDBServiceImpl
    private val tableName = "cantilever-test-content-nodes"

    @BeforeEach
    fun setup() {
        println("Setting up DynamoDBServiceImplTest")
        // Configure the service to use the localstack endpoint
        val region = Region.of(localstack.region)
        val endpoint = URI.create(localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString())

        // Create a client that points to the localstack container
        val dynamoDbClient = DynamoDbAsyncClient.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey)
                )
            )
            .build()

        // Create the test table
        createTable(dynamoDbClient)

        // Initialize the service with the test configuration
        service = DynamoDBServiceImpl(region, tableName, true, dynamoDbClient)
        // You'll need to modify your service to accept a client or use reflection to inject it
    }

    @AfterEach
    fun tearDown() {
        println("Tearing down DynamoDBServiceImplTest")
        // Clean up the table after each test
        val dynamoDbClient = DynamoDbAsyncClient.builder()
            .endpointOverride(
                URI.create(
                    localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString()
                )
            )
            .region(Region.of(localstack.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey)
                )
            )
            .build()

        dynamoDbClient.deleteTable { it.tableName(tableName) }.get()
    }

    @Test
    fun `initial test to verify service setup`() {
        // This test is just to ensure the service is set up correctly
        // You can add more tests to verify specific functionality
        assert(service.tableName == tableName)
    }

    @Test
    fun `saveProject should store project in DynamoDB`() {
        // Arrange
        val project = CantileverProject(
            domain = "test-domain",
            projectName = "Test Project",
            author = "Test Author",
            dateFormat = "yyyy-MM-dd",
            dateTimeFormat = "yyyy-MM-dd HH:mm:ss"
        )

        runBlocking {
            // Act
            println("Saving project")
            val savedProject = service.saveProject(project)

            // Assert
            println("Retrieving project")
            val retrievedProject = service.getProject("test-domain")
            Assertions.assertNotNull(retrievedProject)
            Assertions.assertEquals(project.domain, retrievedProject?.domain)
            Assertions.assertEquals(project.projectName, retrievedProject?.projectName)
        }
    }


    private fun createTable(client: DynamoDbAsyncClient) {
        println("Creating table $tableName in client ${client.serviceName()}")
        val createTableRequest = software.amazon.awssdk.services.dynamodb.model.CreateTableRequest.builder()
            .tableName(tableName)
            .keySchema(
                software.amazon.awssdk.services.dynamodb.model.KeySchemaElement.builder()
                    .attributeName("domain#type")
                    .keyType(software.amazon.awssdk.services.dynamodb.model.KeyType.HASH)
                    .build(),
                software.amazon.awssdk.services.dynamodb.model.KeySchemaElement.builder()
                    .attributeName("srcKey")
                    .keyType(software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE)
                    .build()
            )
            .attributeDefinitions(
                software.amazon.awssdk.services.dynamodb.model.AttributeDefinition.builder()
                    .attributeName("domain#type")
                    .attributeType(software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S)
                    .build(),
                software.amazon.awssdk.services.dynamodb.model.AttributeDefinition.builder()
                    .attributeName("srcKey")
                    .attributeType(software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S)
                    .build(),
                software.amazon.awssdk.services.dynamodb.model.AttributeDefinition.builder()
                    .attributeName("domain")
                    .attributeType(software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S)
                    .build(),
                software.amazon.awssdk.services.dynamodb.model.AttributeDefinition.builder()
                    .attributeName("type#lastUpdated")
                    .attributeType(software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S)
                    .build()
            )
            .globalSecondaryIndexes(
                software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex.builder()
                    .indexName("Project-NodeType-LastUpdated")
                    .keySchema(
                        software.amazon.awssdk.services.dynamodb.model.KeySchemaElement.builder()
                            .attributeName("domain")
                            .keyType(software.amazon.awssdk.services.dynamodb.model.KeyType.HASH)
                            .build(),
                        software.amazon.awssdk.services.dynamodb.model.KeySchemaElement.builder()
                            .attributeName("type#lastUpdated")
                            .keyType(software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE)
                            .build()
                    )
                    .projection(
                        software.amazon.awssdk.services.dynamodb.model.Projection.builder()
                            .projectionType(software.amazon.awssdk.services.dynamodb.model.ProjectionType.ALL)
                            .build()
                    )
                    .provisionedThroughput(
                        software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput.builder()
                            .readCapacityUnits(1L)
                            .writeCapacityUnits(1L)
                            .build()
                    )
                    .build()
            )
            .provisionedThroughput(
                software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput.builder()
                    .readCapacityUnits(1L)
                    .writeCapacityUnits(1L)
                    .build()
            )
            .build()

        client.createTable(createTableRequest).get()
    }

}