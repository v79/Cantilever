package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
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
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class GeneratorControllerTest : KoinTest {

    private val mockS3 : S3Service by inject()
    private val mockSQS: SQSService by inject()
    private val sourceBucket = "sourceBucket"

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module {
                single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
                single<SQSService> { SQSServiceImpl(Region.EU_WEST_2)}
            })
    }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        mockkClass(clazz)
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `responds to request to regenerate page and sends to markdown queue`() {
        val mockSqsResponse = mockk<SendMessageResponse>()
        declareMock<S3Service> {
            every { mockS3.getObjectAsString("sources/pages/about.md", sourceBucket) } returns ""
        }
        declareMock<SQSService> {
            every { mockSQS.sendMessage("markdown_processing_queue", any(), any())} returns mockSqsResponse
        }
        every { mockSqsResponse.messageId()} returns "1234"
        val controller = GeneratorController(sourceBucket)
        val request = buildRequest(path = "/generate/page/about.md", pathPattern = "/generate/page/{srcKey}")

        val response = controller.generatePage(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `responds to request to regenerate post and sends to markdown queue`() {
        val mockSqsResponse = mockk<SendMessageResponse>()
        declareMock<S3Service> {
            every { mockS3.getObjectAsString("sources/posts/my-holiday-post.md", sourceBucket) } returns ""
        }
        declareMock<SQSService> {
            every { mockSQS.sendMessage("markdown_processing_queue", any(), any())} returns mockSqsResponse
        }
        every { mockSqsResponse.messageId()} returns "1234"
        val controller = GeneratorController(sourceBucket)
        val request = buildRequest(path = "/generate/post/my-holiday-post.md", pathPattern = "/generate/page/{srcKey}")

        val response = controller.generatePost(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }


    /**
     * Utility function to build the fake request object
     */
    private fun buildRequest(path: String, pathPattern: String): Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = ""
        apiGatewayProxyRequestEvent.path = path
        return Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }
}