package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock
import org.liamjd.cantilever.common.EnvironmentProvider
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
internal class FileUploadHandlerTest : KoinTest {

    private val mockS3Service: S3Service by inject()
    private val mockSQS: SQSService by inject()
    private val mockDynamoDBService: DynamoDBService by inject()
    private val mockContext: Context = mockk(relaxed = true)
    private val mockLogger = mockk<LambdaLogger>(relaxed = true)
    private val mockEnv = mockk<EnvironmentProvider>()
    private val mockSQSResponse = mockk<software.amazon.awssdk.services.sqs.model.SendMessageResponse> {
        every { messageId() } returns "12345"
    }

    private val postString = """
        ---
        title: My post
        templateKey: sources/templates/post.html.hbs
        date: 2023-01-03
        slug: my-post
        ---
        Post body
    """.trimIndent()

    private val templateString = """
        ---
        name: Test Template
        sections:
          - body
          - sidebar
        ---
        <html>
        <head>
            <title>{{title}}</title>
        </head>
        <body>
            <div class="content">
                {{{body}}}
            </div>
            <div class="sidebar">
                {{{sidebar}}}
            </div>
        </body>
        </html>
    """.trimIndent()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module {
        })
    }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        mockkClass(clazz)
    }

    @BeforeEach
    fun setup() {
        declareMock<Context> {
            every { mockContext.logger } returns mockLogger
        }
        declareMock<EnvironmentProvider> {
            every { mockEnv.getEnv("markdown_processing_queue") } returns "markdown_processing_queue"
            every { mockEnv.getEnv("handlebar_template_queue") } returns "handlebar_template_queue"
            every { mockEnv.getEnv("image_processing_queue")} returns "image_processing_queue"
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `can create a mock of S3Service`() {
        declareMock<S3Service> {

        }
        // This is a simple test to verify that the Koin initialization fix works
        val s3Service = mockS3Service
        assertNotNull(s3Service)
    }

    @Test
    fun `process post upload`(): Unit = runBlocking {
        // setup
        declareMock<S3Service> {
            every {
                mockS3Service.getContentType(
                    "domain.com/sources/posts/my-post.md",
                    "test-bucket"
                )
            } returns "text/markdown"
            every {
                mockS3Service.getObjectAsString("domain.com/sources/posts/my-post.md", "test-bucket")
            } returns postString
        }
        declareMock<DynamoDBService> {
            every { mockDynamoDBService.logger = any() } just runs
            every { mockDynamoDBService.logger } returns mockLogger
            coEvery {
                mockDynamoDBService.upsertContentNode(
                    "domain.com/sources/posts/my-post.md", "domain.com",
                    SOURCE_TYPE.Posts, any<ContentNode.PostNode>(), emptyMap()
                )
            } returns true
        }
        declareMock<SQSService> {
            coEvery { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSQSResponse
        }

        val event = createS3Event("test-bucket", "domain.com/sources/posts/my-post.md", 123L)
        val handler = FileUploadHandler(mockEnv)

        // execute
        val response = handler.handleRequest(event, mockContext)

        // verify
        assertEquals("200 OK", response)
        coVerify {
            mockDynamoDBService.upsertContentNode(
                eq("domain.com/sources/posts/my-post.md"), eq("domain.com"),
                eq(SOURCE_TYPE.Posts),
                any<ContentNode.PostNode>(), any()
            )
        }
    }

    @Test
    fun `process template upload`(): Unit = runBlocking {
        // setup
        declareMock<S3Service> {
            every {
                mockS3Service.getContentType(
                    "domain.com/sources/templates/test-template.hbs",
                    "test-bucket"
                )
            } returns "text/x-handlebars-template"
            every {
                mockS3Service.getObjectAsString("domain.com/sources/templates/test-template.hbs", "test-bucket")
            } returns templateString
        }
        declareMock<DynamoDBService> {
            every { mockDynamoDBService.logger = any() } just runs
            every { mockDynamoDBService.logger } returns mockLogger
            coEvery {
                mockDynamoDBService.upsertContentNode(
                    "domain.com/sources/templates/test-template.hbs", "domain.com",
                    SOURCE_TYPE.Templates, any<ContentNode.TemplateNode>(), any()
                )
            } returns true
        }

        val event = createS3Event("test-bucket", "domain.com/sources/templates/test-template.hbs", 123L)
        val handler = FileUploadHandler(mockEnv)

        // execute
        val response = handler.handleRequest(event, mockContext)

        // verify
        assertEquals("200 OK", response)
        coVerify {
            mockDynamoDBService.upsertContentNode(
                eq("domain.com/sources/templates/test-template.hbs"), eq("domain.com"),
                eq(SOURCE_TYPE.Templates),
                any<ContentNode.TemplateNode>(), any()
            )
        }
    }

    @Test
    fun `process css upload`(): Unit = runBlocking {
        // setup
        declareMock<S3Service> {
            every {
                mockS3Service.getContentType(
                    "domain.com/sources/statics/styles.css",
                    "test-bucket"
                )
            } returns "text/css"
        }
        declareMock<DynamoDBService> {
            every { mockDynamoDBService.logger = any() } just runs
            every { mockDynamoDBService.logger } returns mockLogger
            coEvery {
                mockDynamoDBService.upsertContentNode(
                    "domain.com/sources/statics/styles.css", "domain.com",
                    SOURCE_TYPE.Statics, any<ContentNode.StaticNode>(), emptyMap()
                )
            } returns true
        }
        declareMock<SQSService> {
            coEvery { mockSQS.sendTemplateMessage("handlebar_template_queue", any()) } returns mockSQSResponse
        }

        val event = createS3Event("test-bucket", "domain.com/sources/statics/styles.css", 123L)
        val handler = FileUploadHandler(mockEnv)

        // execute
        val response = handler.handleRequest(event, mockContext)

        // verify
        assertEquals("200 OK", response)
        coVerify {
            mockDynamoDBService.upsertContentNode(
                eq("domain.com/sources/statics/styles.css"), eq("domain.com"),
                eq(SOURCE_TYPE.Statics),
                any<ContentNode.StaticNode>(), any()
            )
        }
        coVerify {
            mockSQS.sendTemplateMessage(
                eq("handlebar_template_queue"),
                any()
            )
        }
    }

    @Test
    fun `process page upload`(): Unit = runBlocking {
        // setup
        val pageString = """
            ---
            title: My Page
            templateKey: sources/templates/page.html.hbs
            slug: my-page
            ---
            Page content
        """.trimIndent()

        declareMock<S3Service> {
            every {
                mockS3Service.getContentType(
                    "domain.com/sources/pages/my-page.md",
                    "test-bucket"
                )
            } returns "text/markdown"
            every {
                mockS3Service.getObjectAsString("domain.com/sources/pages/my-page.md", "test-bucket")
            } returns pageString
        }
        declareMock<DynamoDBService> {
            every { mockDynamoDBService.logger = any() } just runs
            every { mockDynamoDBService.logger } returns mockLogger
            coEvery {
                mockDynamoDBService.upsertContentNode(
                    "domain.com/sources/pages/my-page.md", "domain.com",
                    SOURCE_TYPE.Pages, any<ContentNode.PageNode>(), emptyMap()
                )
            } returns true
        }
        declareMock<SQSService> {
            coEvery { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSQSResponse
        }

        val event = createS3Event("test-bucket", "domain.com/sources/pages/my-page.md", 123L)
        val handler = FileUploadHandler(mockEnv)

        // execute
        val response = handler.handleRequest(event, mockContext)

        // verify
        assertEquals("200 OK", response)
        coVerify {
            mockDynamoDBService.upsertContentNode(
                eq("domain.com/sources/pages/my-page.md"), eq("domain.com"),
                eq(SOURCE_TYPE.Pages),
                any<ContentNode.PageNode>(), any()
            )
        }
        coVerify {
            mockSQS.sendMarkdownMessage(
                eq("markdown_processing_queue"),
                any(),
                any()
            )
        }
    }

    @Test
    fun `process JPG upload`() {
        // setup
        declareMock<S3Service> {
            every {
                mockS3Service.getContentType(
                    "domain.com/sources/images/my-image.jpg",
                    "test-bucket"
                )
            } returns "image/jpeg"
        }
        declareMock<DynamoDBService> {
            every { mockDynamoDBService.logger = any() } just runs
            every { mockDynamoDBService.logger } returns mockLogger
            coEvery {
                mockDynamoDBService.upsertContentNode(
                    "domain.com/sources/images/my-image.jpg", "domain.com",
                    SOURCE_TYPE.Images, any<ContentNode.ImageNode>(), emptyMap()
                )
            } returns true
        }
        declareMock<SQSService> {
            coEvery { mockSQS.sendImageMessage("image_processing_queue", any()) } returns mockSQSResponse
        }

        val event = createS3Event("test-bucket", "domain.com/sources/images/my-image.jpg", 123L)
        val handler = FileUploadHandler(mockEnv)

        // execute
        val response = handler.handleRequest(event, mockContext)

        // verify
        assertEquals("200 OK", response)
        coVerify {
            mockDynamoDBService.upsertContentNode(
                eq("domain.com/sources/images/my-image.jpg"), eq("domain.com"),
                eq(SOURCE_TYPE.Images),
                any<ContentNode.ImageNode>(), any()
            )
        }
        coVerify {
            mockSQS.sendImageMessage(
                eq("image_processing_queue"),
                any()
            )
        }
    }

    @Test
    fun `400 response if a non-markdown post file is uploaded`() {
        // setup
        declareMock<S3Service> {
            every {
                mockS3Service.getContentType(
                    "domain.com/sources/posts/my-post.txt",
                    "test-bucket"
                )
            } returns "text/plain"
        }
        declareMock<DynamoDBService> {
            every { mockDynamoDBService.logger = any() } just runs
            every { mockDynamoDBService.logger } returns mockLogger
        }

        val event = createS3Event("test-bucket", "domain.com/sources/posts/my-post.txt", 123L)
        val handler = FileUploadHandler(mockEnv)

        // execute
        val response = handler.handleRequest(event, mockContext)

        // verify
        assertEquals("400 Bad Request", response)
        coVerify(exactly = 0) { mockDynamoDBService.upsertContentNode(any(), any(), any(), any(), any()) }
    }


    /**
     * Utility function to create a mock S3Event for testing.
     * This simulates an S3 event notification for a file upload.
     */
    private fun createS3Event(bucketName: String, key: String, size: Long): S3Event {
        val s3Object = S3EventNotification.S3ObjectEntity(
            key, size, "eTag", "1", "sequencer"
        )

        val s3Identity = S3EventNotification.UserIdentityEntity("principalId")
        val s3Bucket = S3EventNotification.S3BucketEntity(bucketName, s3Identity, "ownerIdentity")
        val s3Entity = S3EventNotification.S3Entity("configId", s3Bucket, s3Object, "s3SchemaVersion")
        val s3RequestParams = S3EventNotification.RequestParametersEntity("sourceIPAddress")
        val s3ResponseElements = S3EventNotification.ResponseElementsEntity(
            "x-amz-request-id", "x-amz-id-2"
        )
        val s3Record = S3EventNotification.S3EventNotificationRecord(
            "eu-west-2", "eventName", "aws:s3", "12345", "1",
            s3RequestParams, s3ResponseElements,
            s3Entity, s3Identity
        )

        return S3Event(listOf(s3Record))
    }
}