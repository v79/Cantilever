package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.intellij.lang.annotations.Language
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
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.net.URLEncoder
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
    private val generationBucket = "generationBucket"

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
            every { mockS3.getObjectAsString("test/sources/pages/about.md", sourceBucket) } returns "about page text"
            every { mockS3.objectExists("test/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("test/metadata.json", generationBucket) } returns mockMetaJson

        }
        declareMock<SQSService> {
            every { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "1234"
        val controller = GeneratorController(sourceBucket, generationBucket)
        val encoded = URLEncoder.encode("test/sources/pages/about.md", "UTF-8")
        val request = buildRequest(path = "/generate/page/$encoded", pathPattern = "/generate/page/{srcKey}")

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
                templateKey: sources/templates/post
                slug: my-holiday-post
                date: 2023-10-20
                attributes: {}
            """.trimIndent()
            every { mockS3.objectExists("test/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("test/metadata.json", generationBucket) } returns mockMetaJson

        }
        declareMock<SQSService> {
            every { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "1234"
        val controller = GeneratorController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/generate/test/my-holiday-post.md", pathPattern = "/generate/page/{srcKey}")

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
            "srcKey": "test/sources/pages/bio/about-me.md",
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
        "srcKey": "test/sources/pages/folder/",
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
            "srcKey": "test/sources/pages/about.md",
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
            "srcKey": "test/sources/pages/about.md",
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
            "srcKey": "test/sources/pages/index.md",
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
            "srcKey": "test/sources/pages/about.md",
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
            "srcKey": "test/sources/pages/about.md",
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
      "srcKey": "test/sources/posts/momentum-lost.md",
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
            every { mockS3.objectExists("test/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("test/metadata.json", generationBucket) } returns mockMetaJson
            every {
                mockS3.getObjectAsString(
                    "test/sources/pages/dynamodb-design-thoughts.md", sourceBucket
                )
            } returns mockTodoPage
            every { mockS3.getObjectAsString("test/sources/pages/bio/about-me.md", sourceBucket) } returns mockTodoPage
            every { mockS3.getObjectAsString("test/sources/pages/about.md", sourceBucket) } returns mockTodoPage
            every { mockS3.getObjectAsString("test/sources/pages/new-approach.md", sourceBucket) } returns mockTodoPage

            every {
                mockS3.getObjectAsString(
                    "sources/templates/about.html.hbs", sourceBucket
                )
            } returns mockTemplateText
        }
        declareMock<SQSService> {
            every { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "2345"
        val controller = GeneratorController(sourceBucket, generationBucket)
        val request = buildRequest(
            path = "/generate/template/sources%252Ftemplates%252Fabout.html.hbs",
            pathPattern = "/generate/template/{templateKey}"
        )

        val response = controller.generateTemplate(request)

        assertNotNull(response)
        println(response)
        assertEquals(202, response.statusCode)
        verify(exactly = 4) { mockSQS.sendMarkdownMessage(any(), any(), any()) }
    }

    @Test
    fun `responds to request to regenerate all pages with wildcard request`() {
        val mockSqsResponse = mockk<SendMessageResponse>()
        val mockPageListResponse = mockk<ListObjectsV2Response>()
        val mockS3Obj = mockk<S3Object>()
        every { mockPageListResponse.contents() } returns listOf(mockS3Obj)
        every { mockPageListResponse.keyCount() } returns 1
        every { mockS3Obj.key() } returns "test/sources/pages/about.md"
        declareMock<S3Service> {
            every { mockS3.listObjects("test/sources/pages/", sourceBucket) } returns mockPageListResponse
            every { mockS3.getObjectAsString("test/sources/pages/about.md", sourceBucket) } returns ""
            every { mockS3.objectExists("test/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("test/metadata.json", generationBucket) } returns mockPageJsonShort
            every { mockS3.getObjectAsString("test/sources/pages/bio/about-me.md", sourceBucket) } returns ""
        }
        declareMock<SQSService> {
            every { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        every { mockSqsResponse.messageId() } returns "3456"
        val controller = GeneratorController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/generate/page/*", pathPattern = "/generate/page/{srcKey}")

        val response = controller.generatePage(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        val result = response.body as APIResult.Success
        assertEquals("Queued 1 pages for regeneration", result.value)
    }

    @Test
    fun `request delete htmlFragments succeeds`() {
        val mockS3ListResponse = mockk<ListObjectsV2Response>()
        val mockS3Obj = mockk<S3Object>()
        val mockDeleteResponse = mockk<DeleteObjectResponse>()
        declareMock<S3Service> {
            every { mockS3.listObjects("test/generated/htmlFragments/", generationBucket) } returns mockS3ListResponse
            every {
                mockS3.deleteObject(
                    "test/htmlFragments/fragment1.html",
                    generationBucket
                )
            } returns mockDeleteResponse
        }
        every { mockS3ListResponse.hasContents() } returns true
        every { mockS3ListResponse.contents() } returns listOf(mockS3Obj)
        every { mockS3Obj.key() } returns "test/htmlFragments/fragment1.html"

        val controller = GeneratorController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/delete/fragments", pathPattern = "/delete/fragments")

        val response = controller.clearGeneratedFragments(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        val result = response.body as APIResult.Success
        assertEquals("Deleted 1 generated fragments from folder test/generated/htmlFragments/", result.value)
    }

    @Test
    fun `request to delete images fails when all images are still in project metadata`() {
        val imageListResponse = mockk<ListObjectsV2Response>()
        val imageOriginal = S3Object.builder().key("test/generated/images/milkyway.jpg").build()
        val imageThumbnail = S3Object.builder().key("test/generated/images/milkyway/__thumb.jpg").build()
        val imageList = listOf(imageOriginal, imageThumbnail)
        declareMock<S3Service> {
            every { mockS3.objectExists("test/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("test/metadata.json", generationBucket) } returns mockMetaJson
            every { mockS3.listObjects("test/generated/images/", generationBucket) } returns imageListResponse
            every {
                mockS3.listObjects(
                    "test/generated/images/milkyway.jpg",
                    generationBucket
                )
            } returns imageListResponse
        }
        every { imageListResponse.hasContents() } returns true
        every { imageListResponse.contents() } returns imageList

        val controller = GeneratorController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/delete/images", pathPattern = "/delete/images")
        val response = controller.clearGeneratedImages(request)

        assertNotNull(response)
        assertEquals(204, response.statusCode)
        verify {
            mockS3.deleteObject("test/generated/images/milkyway.jpg", generationBucket)?.wasNot(Called)
        }
    }

    @Test
    fun `request to delete images succeeds when images are not referenced in metadata json`() {
        val imageListResponse = mockk<ListObjectsV2Response>()
        val imageOriginal = S3Object.builder().key("test/generated/images/andromeda.jpg").build()
        val imageThumbnail = S3Object.builder().key("test/generated/images/andromeda/__thumb.jpg").build()
        val imageList = listOf(imageOriginal, imageThumbnail)
        declareMock<S3Service> {
            every { mockS3.objectExists("test/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("test/metadata.json", generationBucket) } returns mockMetaJson
            every { mockS3.listObjects("test/generated/images/", generationBucket) } returns imageListResponse
            every {
                mockS3.listObjects(
                    "test/generated/images/andromeda.jpg",
                    generationBucket
                )
            } returns imageListResponse
            every { mockS3.deleteObject(any(), generationBucket) } returns mockk()
        }
        every { imageListResponse.hasContents() } returns true
        every { imageListResponse.contents() } returns imageList

        val controller = GeneratorController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/delete/images", pathPattern = "/delete/images")
        val response = controller.clearGeneratedImages(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        verify(exactly = 2) {
            mockS3.deleteObject(any(), generationBucket)
        }
    }

    /**
     * Utility function to build the fake request object
     */
    private fun buildRequest(path: String, pathPattern: String): org.liamjd.apiviaduct.routing.Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = ""
        apiGatewayProxyRequestEvent.path = path
        apiGatewayProxyRequestEvent.headers = mapOf("cantilever-project-domain" to "test")
        return org.liamjd.apiviaduct.routing.Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }

    @Language("JSON")
    private val mockPageJsonShort = """
        {
  "items": [
    {
      "type": "page",
      "srcKey": "test/sources/pages/about.md",
      "lastUpdated": "2023-11-12T15:24:05.563049390Z",
      "title": "About Cantilever",
      "templateKey": "sources/templates/about.html.hbs",
      "slug": "about",
      "isRoot": false,
      "attributes": {
        "siteName": "Cantilever",
        "author": "Liam Davison"
      },
      "sections": {
        "body": ""
      },
      "parent": "sources/pages"
    }
    ]
    }
    """.trimIndent()

    @Language("JSON")
    private val mockMetaJson = """
{
  "items": [
    {
      "type": "page",
      "srcKey": "test/sources/pages/about.md",
      "lastUpdated": "2023-11-12T15:24:05.563049390Z",
      "title": "About Cantilever",
      "templateKey": "sources/templates/about.html.hbs",
      "slug": "about",
      "isRoot": false,
      "attributes": {
        "siteName": "Cantilever",
        "author": "Liam Davison"
      },
      "sections": {
        "body": ""
      },
      "parent": "sources/pages"
    },
    {
      "type": "page",
      "srcKey": "test/sources/pages/bio/about-me.md",
      "lastUpdated": "2023-11-12T15:24:05.627497495Z",
      "title": "About me",
      "templateKey": "sources/templates/about.html.hbs",
      "slug": "bio/about-me",
      "isRoot": true,
      "attributes": {
      },
      "sections": {
        "body": ""
      },
      "parent": "sources/pages/bio"
    },
    {
      "type": "page",
      "srcKey": "test/sources/pages/dynamodb-design-thoughts.md",
      "lastUpdated": "2023-11-12T15:24:05.735423913Z",
      "title": "DynamoDB Design Thoughts",
      "templateKey": "sources/templates/about.html.hbs",
      "slug": "dynamodb-design-thoughts",
      "isRoot": false,
      "attributes": {
      },
      "sections": {
        "body": ""
      },
      "parent": "sources/pages"
    },
    {
      "type": "page",
      "srcKey": "test/sources/pages/index.md",
      "lastUpdated": "2023-11-12T15:24:05.768820097Z",
      "title": "Cantilever",
      "templateKey": "sources/templates/index.html.hbs",
      "slug": "index",
      "isRoot": false,
      "attributes": {
        "siteName": "Cantilever",
        "author": "Liam John Davison"
      },
      "sections": {
        "body": "",
        "links": ""
      },
      "parent": "sources/pages"
    },
    {
      "type": "page",
      "srcKey": "test/sources/pages/new-approach.md",
      "lastUpdated": "2023-11-12T15:24:05.819663508Z",
      "title": "New Approach",
      "templateKey": "sources/templates/about.html.hbs",
      "slug": "new-approach",
      "isRoot": false,
      "attributes": {
      },
      "sections": {
        "body": ""
      },
      "parent": "sources/pages"
    }
],
"images": [
        {
            "srcKey": "test/sources/images/milkyway.jpg",
            "lastUpdated": "2024-03-26T21:19:11.031397878Z"
        }
    ]
}
            """.trimIndent()
}