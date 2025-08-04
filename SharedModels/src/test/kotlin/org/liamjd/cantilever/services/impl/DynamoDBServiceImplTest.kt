package org.liamjd.cantilever.services.impl

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentNode
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import java.net.URI
import kotlin.test.*

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

    @BeforeTest
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

        // Initialise the service with the test configuration
        service = DynamoDBServiceImpl(region, tableName, dynamoDbClient, true)
        // You'll need to modify your service to accept a client or use reflection to inject it
    }

    @AfterTest
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
        assert(service.tableName == tableName)
    }

    @Test
    fun `getNodeCount should return correct count`() {
        // Setup - Insert dummy nodes
        val post1 = ContentNode.PostNode(
            title = "Test Post 1",
            templateKey = "template1",
            date = LocalDate(2025, 8, 1),
            slug = "test-post-1",
            attributes = emptyMap()
        )
        post1.srcKey = "posts/2025/08/test-post-1.md"

        val post2 = ContentNode.PostNode(
            title = "Test Post 2",
            templateKey = "template2",
            date = LocalDate(2025, 8, 2),
            slug = "test-post-2",
            attributes = emptyMap()
        )
        post2.srcKey = "posts/2025/08/test-post-2.md"

        runBlocking {
            // Insert nodes
            service.upsertContentNode(
                srcKey = post1.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts,
                node = post1,
                attributes = emptyMap()
            )
            service.upsertContentNode(
                srcKey = post2.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts,
                node = post2,
                attributes = emptyMap()
            )

            // Execute
            val count = service.getNodeCount("test-domain", SOURCE_TYPE.Posts)

            // Verify
            assertEquals(2, count, "Node count should be 2")
        }
    }

    @Test
    fun `getNodeCount with no nodes should return zero`() {
        runBlocking {
            // Execute
            val count = service.getNodeCount("test-domain", SOURCE_TYPE.Posts)

            // Verify
            assertEquals(0, count, "Node count should be 0 when no nodes exist")
        }
    }

    @Test
    fun `saveProject should store project in DynamoDB`() {
        // Setup
        val project = CantileverProject(
            domain = "test-domain",
            projectName = "Test Project",
            author = "Test Author",
            dateFormat = "yyyy-MM-dd",
            dateTimeFormat = "yyyy-MM-dd HH:mm:ss"
        )

        runBlocking {
            // Execute
            val savedProject = service.saveProject(project)

            // Verify
            val retrievedProject = service.getProject("test-domain")
            assertNotNull(retrievedProject, "Null project retrieved") {
                assertEquals(project.domain, retrievedProject?.domain)
                assertEquals(project.projectName, retrievedProject?.projectName)
            }
        }
    }

    @Test
    fun `can save a template and list all templates`() {
        // Setup
        val template = ContentNode.TemplateNode(
            srcKey = "sources/templates/myTemplate.hbs",
            lastUpdated = Instant.fromEpochSeconds(100000L),
            title = "My Template",
            sections = listOf("body", "header")
        )

        runBlocking {
            // Execute
            val saved = service.upsertContentNode(
                srcKey = template.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Templates,
                node = template,
                attributes = mapOf(
                    "title" to template.title,
                    "sections" to template.sections.joinToString(",") { it }
                )
            )

            // Verify
            assertTrue(saved)

            val templates = service.listAllTemplates("test-domain")
            println("Retrieved templates: $templates")
            assertNotNull(templates)
            assertTrue(templates.templates.isNotEmpty())
            assertEquals(1, templates.templates.size)
            assertEquals(template.srcKey, templates.templates[0].srcKey)
            assertEquals(template.title, templates.templates[0].title)
            assertEquals(2, templates.templates[0].sections.size)
        }
    }

    @Test
    fun `can save and load a template`() {
        // Setup
        val template = ContentNode.TemplateNode(
            srcKey = "sources/templates/myTemplate.hbs",
            lastUpdated = Instant.fromEpochSeconds(100000L),
            title = "My Template",
            sections = listOf("body", "header")
        )

        runBlocking {
            // Execute
            val saved = service.upsertContentNode(
                srcKey = template.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Templates,
                node = template,
                attributes = mapOf(
                    "title" to template.title,
                    "sections" to template.sections.joinToString(",") { it }
                )
            )

            // Verify
            assertTrue(saved, "Failed to save template. Check the logs.")

            val retrievedTemplate = service.getContentNode(
                srcKey = template.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Templates
            )
            assertNotNull(retrievedTemplate)
            assertIs<ContentNode.TemplateNode>(retrievedTemplate)
            assertEquals(template.srcKey, retrievedTemplate.srcKey)
            assertEquals(template.title, retrievedTemplate.title)
            assertEquals(template.sections, retrievedTemplate.sections)
        }
    }

    @Test
    fun `can save a post`() {
        // Setup
        val post = ContentNode.PostNode(
            title = "Test Post",
            templateKey = "sources/templates/post.html.hbs",
            date = LocalDate(2025, 7, 30),
            slug = "test-post",
            attributes = mapOf(
                "author" to "Test Author",
                "category" to "Test Category",
                "tags" to "test,post,unit-test"
            )
        )
        post.srcKey = "sources/posts/2025/07/test-post.md"
        post.body = "This is the body of the test post."
        post.next = "sources/posts/2025/08/next-post.md"
        post.prev = "sources/posts/2025/06/previous-post.md"

        runBlocking {
            // Execute
            val saved = service.upsertContentNode(
                srcKey = post.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts,
                node = post,
                attributes = mapOf(
                    "title" to post.title,
                    "templateKey" to post.templateKey,
                    "date" to post.date.toString(),
                    "slug" to post.slug,
                    "body" to post.body,
                    "author" to (post.attributes["author"] ?: ""),
                    "category" to (post.attributes["category"] ?: ""),
                    "tags" to (post.attributes["tags"] ?: "")
                )
            )

            // Verify
            assertTrue(saved, "Failed to save post. Check the logs.")
        }
    }

    @Test
    fun `can save a post with custom attributes`() {
        // Setup
        val post = ContentNode.PostNode(
            title = "Custom Attributes Post",
            templateKey = "sources/templates/post.html.hbs",
            date = LocalDate(2025, 7, 30),
            slug = "custom-attributes-post",
            attributes = mapOf(
                "author" to "Test Author",
                "category" to "Test Category",
                "tags" to "test,post,custom-attributes"
            )
        )
        post.srcKey = "sources/posts/2025/07/custom-attributes-post.md"
        post.body = "This is a post with custom attributes."

        runBlocking {
            // Execute - Include standard attributes and additional custom attributes
            val saved = service.upsertContentNode(
                srcKey = post.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts,
                node = post,
                attributes = mapOf(
                    // Attributes from the attributes map
                    "author" to (post.attributes["author"] ?: ""),
                    "category" to (post.attributes["category"] ?: ""),
                    "tags" to (post.attributes["tags"] ?: ""),

                    // Custom attributes not part of the standard PostNode properties
                    "featured" to "true",
                    "readingTime" to "5 minutes",
                    "coverImage" to "sources/images/cover.jpg",
                    "metaDescription" to "This is a meta description for SEO purposes",
                    "publishStatus" to "published"
                )
            )

            // Verify
            assertTrue(saved, "Failed to save post with custom attributes. Check the logs.")

            val retrievedPost = service.getContentNode(
                srcKey = post.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts
            )
            assertNotNull(retrievedPost)
            assertIs<ContentNode.PostNode>(retrievedPost)
            assertEquals(post.srcKey, retrievedPost.srcKey)
            assertEquals(post.title, retrievedPost.title)
            assertEquals(post.slug, retrievedPost.slug)
        }
    }

    @Test
    fun `can save and load static content node`() {
        // Setup
        val staticNode = ContentNode.StaticNode(
            srcKey = "sources/static/styles.css",
            lastUpdated = Instant.fromEpochSeconds(100000L)
        )

        runBlocking {
            // Execute
            val saved = service.upsertContentNode(
                srcKey = staticNode.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Statics,
                node = staticNode,
                attributes = emptyMap() // Static nodes only have srcKey and lastUpdated
            )

            // Verify
            assertTrue(saved, "Failed to save static node. Check the logs.")

            val retrievedNode = service.getContentNode(
                srcKey = staticNode.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Statics
            )

            // Verify the retrieved node
            assertNotNull(retrievedNode, "Retrieved node should not be null")
            assertIs<ContentNode.StaticNode>(retrievedNode, "Retrieved node should be a StaticNode")
            assertEquals(staticNode.srcKey, retrievedNode.srcKey, "Source keys should match")
            // Note: We don't check lastUpdated because it might be updated during the save process
        }
    }
    
    @Test
    fun `can list all posts for a project`() {
        // Setup - Create multiple posts for the same domain
        val post1 = ContentNode.PostNode(
            title = "First Test Post",
            templateKey = "sources/templates/post.html.hbs",
            date = LocalDate(2025, 7, 30),
            slug = "first-test-post",
            attributes = mapOf("author" to "Test Author")
        )
        post1.srcKey = "sources/posts/2025/07/first-test-post.md"
        
        val post2 = ContentNode.PostNode(
            title = "Second Test Post",
            templateKey = "sources/templates/post.html.hbs",
            date = LocalDate(2025, 8, 1),
            slug = "second-test-post",
            attributes = mapOf("author" to "Test Author")
        )
        post2.srcKey = "sources/posts/2025/08/second-test-post.md"
        
        runBlocking {
            // Save the posts
            service.upsertContentNode(
                srcKey = post1.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts,
                node = post1,
                attributes = mapOf("title" to post1.title)
            )
            
            service.upsertContentNode(
                srcKey = post2.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts,
                node = post2,
                attributes = mapOf("title" to post2.title)
            )
            
            // Execute - List all posts for the domain
            val posts = service.listAllPostsForProject("test-domain")
            
            // Verify
            assertNotNull(posts, "Posts list should not be null")
            assertEquals(2, posts.size, "Should have found 2 posts")
            
            // Verify the posts contain the expected data
            val foundPost1 = posts.find { it.srcKey == post1.srcKey }
            val foundPost2 = posts.find { it.srcKey == post2.srcKey }
            
            assertNotNull(foundPost1, "First post should be in the results")
            assertNotNull(foundPost2, "Second post should be in the results")
            
            assertEquals(post1.title, foundPost1?.title, "First post title should match")
            assertEquals(post2.title, foundPost2?.title, "Second post title should match")
        }
    }

    @Test
    fun `can delete an existing content node`() {
        // Setup
        val nodeKey = "sources/posts/2025/07/sample-post.md"
        val post = ContentNode.PostNode(
            title = "Sample Post",
            templateKey = "sources/templates/post.html.hbs",
            date = LocalDate(2025, 7, 30),
            slug = "sample-post",
            attributes = mapOf("author" to "Test Author")
        )
        post.srcKey = nodeKey

        runBlocking {
            service.upsertContentNode(
                srcKey = post.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Posts,
                node = post,
                attributes = emptyMap()
            )

            // Execute
            service.deleteContentNode(
                srcKey = nodeKey, projectDomain = "test-domain", contentType = SOURCE_TYPE.Posts
            )

            // Verify
            val deletedNode = service.getContentNode(
                srcKey = nodeKey, projectDomain = "test-domain", contentType = SOURCE_TYPE.Posts
            )
            assertNull(deletedNode, "Deleted node should not be retrievable")
        }
    }

    @Test
    fun `can delete a static content node`() {
        // Setup
        val staticKey = "sources/static/sample-style.css"
        val staticNode = ContentNode.StaticNode(
            srcKey = staticKey,
            lastUpdated = Instant.fromEpochSeconds(100000L)
        )
        runBlocking {
            service.upsertContentNode(
                srcKey = staticNode.srcKey,
                projectDomain = "test-domain",
                contentType = SOURCE_TYPE.Statics,
                node = staticNode,
                attributes = emptyMap()
            )

            // Execute
            val isDeleted = service.deleteContentNode(
                srcKey = staticKey, projectDomain = "test-domain", contentType = SOURCE_TYPE.Statics
            )

            // Verify
            val deletedStaticNode = service.getContentNode(
                srcKey = staticKey, projectDomain = "test-domain", contentType = SOURCE_TYPE.Statics
            )
            assertNull(deletedStaticNode, "Deleted static node should not be retrievable")
        }
    }


    /**
     * Create the DynamoDB table for testing. This is a pretty hand-coded representation of the table structure
     * that the service expects. The real table is created in the CDK scripts.
     * Keeping these in sync could be a problem in the future.
     */
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