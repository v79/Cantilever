package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
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
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * These tests are really just happy paths for now
 */
@ExtendWith(MockKExtension::class)
class GeneratorControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockSQS: SQSService by inject()
    private val sourceBucket = "sourceBucket"

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module {
            single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
            single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
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
            every { mockSQS.sendMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "1234"
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
            every { mockS3.getObjectAsString("sources/posts/my-holiday-post.md", sourceBucket) } returns """
                title: My holiday post
                templateKey: post
                slug: my-holiday-post
                date: 2023-10-20
            """.trimIndent()
        }
        declareMock<SQSService> {
            every { mockSQS.sendMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "1234"
        val controller = GeneratorController(sourceBucket)
        val request = buildRequest(path = "/generate/post/my-holiday-post.md", pathPattern = "/generate/page/{srcKey}")

        val response = controller.generatePost(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `responds to request to regenerate pages based on a template`() {
        val mockSqsResponse = mockk<SendMessageResponse>()
        val mockPageJson = """
       {
  "lastUpdated": "2023-10-20T09:50:13.895554407Z",
  "container": {
    "srcKey": "sources/pages/",
    "children": [
      {
        "type": "folder",
        "srcKey": "sources/pages/bio/",
        "children": [
          {
            "type": "page",
            "title": "About me",
            "srcKey": "sources/pages/bio/about-me.md",
            "templateKey": "sources/templates/about.html.hbs",
            "url": "bio/about-me",
            "attributes": {},
            "sections": {
              "body": ""
            },
            "lastUpdated": "2023-10-19T19:03:59Z"
          }
        ],
        "isRoot": false,
        "count": 1
      },
      {
        "type": "folder",
        "srcKey": "sources/pages/folder/",
        "children": null,
        "isRoot": false,
        "count": 0
      },
      {
        "type": "folder",
        "srcKey": "sources/pages/",
        "children": [
          {
            "type": "page",
            "title": "About Cantilever",
            "srcKey": "sources/pages/about.md",
            "templateKey": "sources/templates/about.html.hbs",
            "url": "about",
            "attributes": {
              "siteName": "Cantilever",
              "author": "Liam Davison"
            },
            "sections": {
              "body": ""
            },
            "lastUpdated": "2023-10-20T09:50:05Z"
          },
          {
            "type": "page",
            "title": "Testing Emoji",
            "srcKey": "sources/pages/about.md",
            "templateKey": "sources/templates/about.html.hbs",
            "url": "emoji",
            "attributes": {
              "siteName": "Cantilever",
              "author": "Liam Davison"
            },
            "sections": {
              "body": ""
            },
            "lastUpdated": "2023-10-20T09:49:59Z"
          },
          {
            "type": "page",
            "title": "Cantilever",
            "srcKey": "sources/pages/index.md",
            "templateKey": "index",
            "url": "index",
            "attributes": {
              "siteName": "Cantilever",
              "author": "Liam John Davison"
            },
            "sections": {
              "body": "",
              "blockA": "",
              "links": ""
            },
            "lastUpdated": "2023-08-19T16:36:08Z"
          },
          {
            "type": "page",
            "title": "Page Model",
            "srcKey": "sources/pages/about.md",
            "templateKey": "sources/templates/about.html.hbs",
            "url": "page-model",
            "attributes": {},
            "sections": {
              "body": ""
            },
            "lastUpdated": "2023-10-20T07:07:55Z"
          },
          {
            "type": "page",
            "title": "Todo",
            "srcKey": "sources/pages/about.md",
            "templateKey": "sources/templates/about.html.hbs",
            "url": "todo",
            "attributes": {
              "siteName": "Cantilever",
              "author": "Liam Davison"
            },
            "sections": {
              "body": ""
            },
            "lastUpdated": "2023-10-20T09:49:49Z"
          }
        ],
        "isRoot": false,
        "count": 5
      }
    ],
    "isRoot": false,
    "count": 9
  }
}
        """.trimIndent()

        val mockPostJson = """
            {
  "count": 1,
  "lastUpdated": "2023-07-03T19:46:47.042018Z",
  "posts": [
    {
      "title": "Momentum lost",
      "srcKey": "sources/posts/momentum-lost.md",
      "url": "momentum-lost",
      "date": "2023-05-13",
      "lastUpdated": "2023-07-03T19:46:46.686504Z",
      "templateKey": "templates/post.html.hbs"
        }
  ]
}
        """.trimIndent()
        val mockTodoPage = ""
        val mockTemplateText = ""

        declareMock<S3Service> {
            every { mockS3.objectExists(any(), sourceBucket) } returns true
            every { mockS3.getObjectAsString("generated/pages.json", sourceBucket) } returns mockPageJson
            every { mockS3.getObjectAsString("generated/posts.json", sourceBucket) } returns mockPostJson
            every { mockS3.getObjectAsString("sources/pages/bio/about-me.md", sourceBucket) } returns mockTodoPage
            every { mockS3.getObjectAsString("sources/pages/about.md", sourceBucket) } returns mockTodoPage
            every {
                mockS3.getObjectAsString(
                    "sources/templates/about.html.hbs",
                    sourceBucket
                )
            } returns mockTemplateText
        }
        declareMock<SQSService> {
            every { mockSQS.sendMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "2345"
        val controller = GeneratorController(sourceBucket)
        val request = buildRequest(
            path = "/generate/template/sources%252Ftemplates%252Fabout.html.hbs",
            pathPattern = "/generate/template/{templateKey}"
        )

        val response = controller.generateTemplate(request)

        assertNotNull(response)
        println(response)
        assertEquals(202, response.statusCode)
        verify(exactly = 5) { mockSQS.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `responds to request to regenerate all pages with wildcard request`() {
        val mockSqsResponse = mockk<SendMessageResponse>()
        val mockPageListResponse = mockk<ListObjectsV2Response>()
        val mockS3Obj = mockk<S3Object>()
        every { mockPageListResponse.contents() } returns listOf(mockS3Obj)
        every { mockPageListResponse.keyCount() } returns 1
        every { mockS3Obj.key() } returns "sources/pages/about.md"
        declareMock<S3Service> {
            every { mockS3.listObjects("sources/pages/", sourceBucket) } returns mockPageListResponse
            every { mockS3.getObjectAsString("sources/pages/about.md", sourceBucket) } returns ""
        }
        declareMock<SQSService> {
            every { mockSQS.sendMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "3456"
        val controller = GeneratorController(sourceBucket)
        val request = buildRequest(path = "/generate/page/*", pathPattern = "/generate/page/{srcKey}")

        val response = controller.generatePage(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        val result = response.body as APIResult.Success
        assertEquals("1 pages have been regenerated", result.value)
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