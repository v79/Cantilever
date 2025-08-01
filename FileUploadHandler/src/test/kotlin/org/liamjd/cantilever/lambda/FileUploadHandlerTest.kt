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
            coEvery { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any())} returns mockSQSResponse
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