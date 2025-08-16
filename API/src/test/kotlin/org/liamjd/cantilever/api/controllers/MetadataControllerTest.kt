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
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.S3Object
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
internal class MetadataControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockDynamo: DynamoDBService by inject()
    private val sourceBucket = "sourceBucket"
    private val generationBucket = "generationBucket"

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module { })
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
    fun `adds folder chain for nested page and sets child on immediate parent`() {
        val listResponse = mockk<ListObjectsV2Response>()
        // one nested page under pages folder
        val pageObj = S3Object.builder().key("test/sources/pages/section/subsection/page.md").build()
        every { listResponse.hasContents() } returns true
        every { listResponse.contents() } returns listOf(pageObj)

        declareMock<S3Service> {
            every { mockS3.listObjects("test/sources/", sourceBucket) } returns listResponse
            every { mockS3.getObjectAsString("test/sources/pages/section/subsection/page.md", sourceBucket) } returns "" // minimal
        }

        // Capture folder upserts to verify chain and children/indexPage
        declareMock<DynamoDBService> {
            coEvery { mockDynamo.upsertContentNode(any(), any(), any(), any(), any()) } returns true
        }

        val controller = MetadataController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/metadata/rebuild", pathPattern = "/metadata/rebuild")

        val response = controller.rebuildFromSources(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        // Verify folder chain created for section/ and section/subsection/
        coVerify(exactly = 1) {
            mockDynamo.upsertContentNode(
                "test/sources/pages/section/",
                "test",
                SOURCE_TYPE.Folders,
                withArg { node ->
                    // immediate parent should contain the page as a child
                    // but this is the first level; no specific child assertion here
                },
                any()
            )
        }
        coVerify(exactly = 1) {
            mockDynamo.upsertContentNode(
                "test/sources/pages/section/subsection/",
                "test",
                SOURCE_TYPE.Folders,
                withArg { node ->
                    // The immediate parent folder (subsection) should include the page as a child
                    val children = (node as org.liamjd.cantilever.models.ContentNode.FolderNode).children
                    assert(children.contains("test/sources/pages/section/subsection/page.md"))
                    // isRoot defaults to false, so indexPage should be empty or null
                    val index = node.indexPage
                    if (index != null) {
                        assert(index.isEmpty())
                    }
                },
                any()
            )
        }
    }

    @Test
    fun `sets indexPage on folder when page is root`() {
        val listResponse = mockk<ListObjectsV2Response>()
        val pageObj = S3Object.builder().key("test/sources/pages/landing/index.md").build()
        every { listResponse.hasContents() } returns true
        every { listResponse.contents() } returns listOf(pageObj)

        // Front matter indicating isRoot true so builder marks page.isRoot
        val markdown = """
            ---
            title: Landing
            templateKey: sources/templates/index.html.hbs
            slug: index
            isRoot: true
            ---
        """.trimIndent()

        declareMock<S3Service> {
            every { mockS3.listObjects("test/sources/", sourceBucket) } returns listResponse
            every { mockS3.getObjectAsString("test/sources/pages/landing/index.md", sourceBucket) } returns markdown
        }

        declareMock<DynamoDBService> {
            coEvery { mockDynamo.upsertContentNode(any(), any(), any(), any(), any()) } returns true
        }

        val controller = MetadataController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/metadata/rebuild", pathPattern = "/metadata/rebuild")

        val response = controller.rebuildFromSources(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        val body = response.body as APIResult.Success
        // sanity: something processed
        assert(body.value.contains("pages"))

        // Verify that folder for landing/ is upserted with indexPage set to the page srcKey
        coVerify(atLeast = 1) {
            mockDynamo.upsertContentNode(
                eq("test/sources/pages/landing/"),
                eq("test"),
                eq(SOURCE_TYPE.Folders),
                withArg { node ->
                    val folder = node as org.liamjd.cantilever.models.ContentNode.FolderNode
                    assertEquals("test/sources/pages/landing/index.md", folder.indexPage)
                },
                any()
            )
        }
    }

    private fun buildRequest(path: String, pathPattern: String): org.liamjd.apiviaduct.routing.Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = ""
        apiGatewayProxyRequestEvent.path = path
        apiGatewayProxyRequestEvent.headers = mapOf("cantilever-project-domain" to "test")
        return org.liamjd.apiviaduct.routing.Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }
}