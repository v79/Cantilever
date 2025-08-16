package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
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
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
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
internal class GeneratorControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockSQS: SQSService by inject()
    private val mockDynamoDBService: DynamoDBService by inject()
    private val sourceBucket = "sourceBucket"
    private val generationBucket = "generationBucket"

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
        val post = ContentNode.PostNode(
            srcKey = "test/sources/post/my-holiday-post.md",
            title = "My holiday post",
            templateKey = "sources/templates/post.html.hbs",
            date = LocalDate.parse(
                "2023-10-20"
            ),
            slug = "my-holiday-post",
            attributes = emptyMap(),
            lastUpdated = Clock.System.now()
        )
        declareMock<S3Service> {
            every { mockS3.getObjectAsString("test/sources/posts/my-holiday-post.md", sourceBucket) } returns """
                title: My holiday post
                templateKey: sources/templates/post.html.hbs
                slug: my-holiday-post
                date: 2023-10-20
                attributes: {}
            """.trimIndent()
        }
        declareMock<SQSService> {
            every { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        declareMock<DynamoDBService> {
            coEvery { mockDynamoDBService.listAllNodesForProject("test", SOURCE_TYPE.Posts) } returns listOf(post)
        }
        every { mockSqsResponse.messageId() } returns "1234"

        val controller = GeneratorController(sourceBucket, generationBucket)
        val encoded = URLEncoder.encode("test/sources/posts/my-holiday-post.md", "UTF-8")
        val request = buildRequest(path = "/generate/post/$encoded", pathPattern = "/generate/post/{srcKey}")

        val response = controller.generatePost(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `responds to request to regenerate pages based on a template`() {
        val mockSqsResponse = mockk<SendMessageResponse>()
        val mockTodoPage = ""
        val mockTemplateText = ""
        val page1 = buildPageNode("test/sources/pages/dynamodb-design-thoughts.md")
        val page2 = buildPageNode("test/sources/pages/dynamodb-design-thoughts.md")
        val page3 = buildPageNode("test/sources/pages/about.md")
        val page4 = buildPageNode("test/sources/pages/new-approach.md.md")

        declareMock<S3Service> {
            every { mockS3.objectExists(any(), sourceBucket) } returns true
            every {
                mockS3.getObjectAsString(
                    page1.srcKey, sourceBucket
                )
            } returns mockTodoPage
            every { mockS3.getObjectAsString(page2.srcKey, sourceBucket) } returns mockTodoPage
            every { mockS3.getObjectAsString(page3.srcKey, sourceBucket) } returns mockTodoPage
            every { mockS3.getObjectAsString(page4.srcKey, sourceBucket) } returns mockTodoPage

            every {
                mockS3.getObjectAsString(
                    "sources/templates/about.html.hbs", sourceBucket
                )
            } returns mockTemplateText
        }
        declareMock<SQSService> {
            every { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        declareMock<DynamoDBService> {
            coEvery {
                mockDynamoDBService.getKeyListMatchingTemplate(
                    "test",
                    SOURCE_TYPE.Pages,
                    "sources/templates/about.html.hbs"
                )
            } returns listOf(page1.srcKey, page2.srcKey, page3.srcKey, page4.srcKey)
            coEvery {
                mockDynamoDBService.getKeyListMatchingTemplate(
                    "test",
                    SOURCE_TYPE.Posts,
                    "sources/templates/about.html.hbs"
                )
            } returns emptyList()
            coEvery {
                mockDynamoDBService.getContentNode(page1.srcKey, "test", SOURCE_TYPE.Pages)
            } returns page1
            coEvery {
                mockDynamoDBService.getContentNode(page2.srcKey, "test", SOURCE_TYPE.Pages)
            } returns page2
            coEvery {
                mockDynamoDBService.getContentNode(page3.srcKey, "test", SOURCE_TYPE.Pages)
            } returns page3
            coEvery {
                mockDynamoDBService.getContentNode(page4.srcKey, "test", SOURCE_TYPE.Pages)
            } returns page4
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
        val page = ContentNode.PageNode(
            srcKey = "test/sources/pages/about.md",
            lastUpdated = Clock.System.now(),
            title = "About me",
            templateKey = "about.html.hbs",
            slug = "about-me",
            isRoot = false,
            attributes = emptyMap(),
            sections = emptyMap(),
            parent = ""
        )
        every { mockPageListResponse.contents() } returns listOf(mockS3Obj)
        every { mockPageListResponse.keyCount() } returns 1
        every { mockS3Obj.key() } returns "test/sources/pages/about.md"
        declareMock<S3Service> {
            every { mockS3.listObjects("test/sources/pages/", sourceBucket) } returns mockPageListResponse
            every { mockS3.getObjectAsString("test/sources/pages/about.md", sourceBucket) } returns ""
            every { mockS3.getObjectAsString("test/sources/pages/bio/about-me.md", sourceBucket) } returns ""
        }
        declareMock<SQSService> {
            every { mockSQS.sendMarkdownMessage("markdown_processing_queue", any(), any()) } returns mockSqsResponse
        }
        declareMock<DynamoDBService> {
            coEvery {
                mockDynamoDBService.listAllNodesForProject("test", SOURCE_TYPE.Pages)
            } returns listOf(page)
        }
        every { mockSqsResponse.messageId() } returns "3456"
        val controller = GeneratorController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/generate/page/*", pathPattern = "/generate/page/{srcKey}")

        val response = runBlocking { controller.generatePage(request) }

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
            every { mockS3.objectExists("test/generated/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("test/generated/metadata.json", generationBucket) } returns mockMetaJson
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
        verify(exactly = 0) {
            mockS3.deleteObject("test/generated/images/milkyway.jpg", generationBucket)
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

    /**
     * Utility function to build a ContentNode.PageNode
     */
    private fun buildPageNode(srcKey: String): ContentNode.PageNode = ContentNode.PageNode(
        srcKey = srcKey,
        title = srcKey,
        templateKey = "test/sources/templates/about.html.hbs",
        slug = srcKey,
        lastUpdated = Clock.System.now(),
        attributes = emptyMap(),
        sections = mapOf("body" to ""),
        isRoot = false,
    )

    @Language("JSON")
    private val mockPageJsonShort = """
        {
  "items": [
    {
      "type": "page",
      "srcKey": "test/sources/pages/about.md",
      "lastUpdated": "2023-11-12T15:24:05.563049390Z",
      "title": "About Cantilever",
      "templateKey": "test/sources/templates/about.html.hbs",
      "slug": "about",
      "isRoot": false,
      "attributes": {
        "siteName": "Cantilever",
        "author": "Liam Davison"
      },
      "sections": {
        "body": ""
      },
      "parent": "test/sources/pages"
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
      "templateKey": "test/sources/templates/about.html.hbs",
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
      "templateKey": "test/sources/templates/about.html.hbs",
      "slug": "bio/about-me",
      "isRoot": true,
      "attributes": {
      },
      "sections": {
        "body": ""
      },
      "parent": "test/sources/pages/bio"
    },
    {
      "type": "page",
      "srcKey": "test/sources/pages/dynamodb-design-thoughts.md",
      "lastUpdated": "2023-11-12T15:24:05.735423913Z",
      "title": "DynamoDB Design Thoughts",
      "templateKey": "test/sources/templates/about.html.hbs",
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
      "templateKey": "test/sources/templates/index.html.hbs",
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
      "parent": "test/sources/pages"
    },
    {
      "type": "page",
      "srcKey": "test/sources/pages/new-approach.md",
      "lastUpdated": "2023-11-12T15:24:05.819663508Z",
      "title": "New Approach",
      "templateKey": "test/sources/templates/about.html.hbs",
      "slug": "new-approach",
      "isRoot": false,
      "attributes": {
      },
      "sections": {
        "body": ""
      },
      "parent": "test/sources/pages"
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