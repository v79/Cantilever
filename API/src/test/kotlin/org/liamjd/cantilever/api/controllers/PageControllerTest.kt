package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
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
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.PageTreeDTO
import org.liamjd.cantilever.models.rest.ReassignIndexRequestDTO
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
internal class PageControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockDynamoDB: DynamoDBService by inject()
    private val sourceBucket = "sourceBucket"
    private val generationBucket = "generationBucket"

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module {})
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
    fun `getPages returns 200 with list from DB`() {
        val domain = "example.com"
        val pages = listOf(
            ContentNode.PageNode(
                srcKey = "$domain/sources/pages/home/index.md",
                title = "Home",
                templateKey = "sources/templates/page.hbs",
                slug = "index.html",
                isRoot = true,
                attributes = emptyMap(),
                sections = mapOf("main" to "Hello")
            ),
            ContentNode.PageNode(
                srcKey = "$domain/sources/pages/about.md",
                title = "About",
                templateKey = "sources/templates/page.hbs",
                slug = "about.html",
                isRoot = false,
                attributes = emptyMap(),
                sections = mapOf("main" to "About text")
            )
        )

        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.listAllNodesForProject(domain, SOURCE_TYPE.Pages) } returns pages
        }

        val controller = PageController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/pages", pathPattern = "/pages", headers = mapOf("cantilever-project-domain" to domain))

        val response = controller.getPages(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `loadMarkdownSource returns 200 when file exists`() {
        val domain = "example.com"
        val srcKey = "$domain/sources/pages/home/index.md"
        val markdown = """
            ---
            title: Home
            templateKey: sources/templates/page.hbs
            --- #main
            Hello world
        """.trimIndent()

        declareMock<S3Service> {
            every { mockS3.objectExists(srcKey, sourceBucket) } returns true
            every { mockS3.getObjectAsString(srcKey, sourceBucket) } returns markdown
        }

        val controller = PageController(sourceBucket, generationBucket)
        val encoded = java.net.URLEncoder.encode(srcKey, java.nio.charset.Charset.defaultCharset())
        val request = buildRequest(path = "/page/${encoded}", pathPattern = "/page/{srcKey}", headers = mapOf("cantilever-project-domain" to domain))

        val response = controller.loadMarkdownSource(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `createFolder returns 200 when folder created`() {
        val domain = "example.com"
        val folder = "$domain/sources/pages/new-folder/"

        declareMock<S3Service> {
            every { mockS3.objectExists(folder, sourceBucket) } returns false
            every { mockS3.createFolder(folder, sourceBucket) } returns 0
        }

        val controller = PageController(sourceBucket, generationBucket)
        val encoded = java.net.URLEncoder.encode(folder, java.nio.charset.Charset.defaultCharset())
        val request = buildRequest(path = "/page/folder/${encoded}", pathPattern = "/page/folder/{folderName}", headers = mapOf("cantilever-project-domain" to domain))

        val response = controller.createFolder(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `saveMarkdownPageSource returns 200 when updating existing page`() {
        val domain = "example.com"
        val page = ContentNode.PageNode(
            srcKey = "$domain/sources/pages/home/index.md",
            title = "Home",
            templateKey = "sources/templates/page.hbs",
            slug = "index.html",
            isRoot = true,
            attributes = mapOf("a" to "1"),
            sections = mapOf("main" to "Hello")
        )

        declareMock<S3Service> {
            every { mockS3.objectExists(page.srcKey, sourceBucket) } returns true
            every { mockS3.putObjectAsString(page.srcKey, sourceBucket, any(), "text/markdown") } returns 100
        }

        val controller = PageController(sourceBucket, generationBucket)
        val apiProxyEvent = APIGatewayProxyRequestEvent()
        apiProxyEvent.headers = mapOf("cantilever-project-domain" to domain)
        val request = org.liamjd.apiviaduct.routing.Request(
            apiRequest = apiProxyEvent,
            body = page,
            pathPattern = "/page/save"
        )

        val response = controller.saveMarkdownPageSource(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `saveMarkdownPageSource returns 200 when creating new page`() {
        val domain = "example.com"
        val page = ContentNode.PageNode(
            srcKey = "$domain/sources/pages/new-page.md",
            title = "New",
            templateKey = "sources/templates/page.hbs",
            slug = "new.html",
            isRoot = false,
            attributes = emptyMap(),
            sections = mapOf("main" to "Body")
        )

        declareMock<S3Service> {
            every { mockS3.objectExists(page.srcKey, sourceBucket) } returns false
            every { mockS3.putObjectAsString(page.srcKey, sourceBucket, any(), "text/markdown") } returns 120
        }

        val controller = PageController(sourceBucket, generationBucket)
        val apiProxyEvent = APIGatewayProxyRequestEvent()
        apiProxyEvent.headers = mapOf("cantilever-project-domain" to domain)
        val request = org.liamjd.apiviaduct.routing.Request(
            apiRequest = apiProxyEvent,
            body = page,
            pathPattern = "/page/save"
        )

        val response = controller.saveMarkdownPageSource(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `deleteMarkdownPageSource returns 200 when file exists and deleted`() {
        val domain = "example.com"
        val srcKey = "$domain/sources/pages/delete-me.md"

        declareMock<S3Service> {
            every { mockS3.objectExists(srcKey, sourceBucket) } returns true
            every { mockS3.deleteObject(srcKey, sourceBucket) } returns mockk()
        }

        val controller = PageController(sourceBucket, generationBucket)
        val encoded = java.net.URLEncoder.encode(srcKey, java.nio.charset.Charset.defaultCharset())
        val request = buildRequest(path = "/page/${encoded}", pathPattern = "/page/{srcKey}", headers = mapOf("cantilever-project-domain" to domain))

        val response = controller.deleteMarkdownPageSource(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `getFolders returns 200 with list from DB`() {
        val domain = "example.com"
        val folders = listOf(
            ContentNode.FolderNode(srcKey = "$domain/sources/pages/a/", lastUpdated = Clock.System.now()),
            ContentNode.FolderNode(srcKey = "$domain/sources/pages/b/", lastUpdated = Clock.System.now())
        )

        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.listAllNodesForProject(domain, SOURCE_TYPE.Folders) } returns folders
        }

        val controller = PageController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/pages/folders", pathPattern = "/pages/folders", headers = mapOf("cantilever-project-domain" to domain))

        val response = controller.getFolders(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `deleteFolder returns 200 when folder exists and is empty`() {
        val domain = "example.com"
        val folderKey = "$domain/sources/pages/empty/"
        val folderNode = ContentNode.FolderNode(srcKey = folderKey, children = mutableListOf(), lastUpdated = Clock.System.now())

        declareMock<S3Service> {
            every { mockS3.objectExists(folderKey, sourceBucket) } returns true
            every { mockS3.deleteObject(folderKey, sourceBucket) } returns mockk()
        }
        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.getContentNode(folderKey, domain, SOURCE_TYPE.Folders) } returns folderNode
        }

        val controller = PageController(sourceBucket, generationBucket)
        val encoded = java.net.URLEncoder.encode(folderKey, java.nio.charset.Charset.defaultCharset())
        val request = buildRequest(path = "/page/folder/${encoded}", pathPattern = "/page/folder/{srcKey}", headers = mapOf("cantilever-project-domain" to domain))

        val response = controller.deleteFolder(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `getPageTree builds hierarchical tree with root and nested pages`() {
        val domain = "example.com"
        val now = Clock.System.now()
        val rootFolderKey = "$domain/sources/pages"
        val blogFolderKey = "$domain/sources/pages/blog" // no trailing slash to match parent calculation

        val folders = listOf(
            // Only need to include non-root folders; root is provided by controller/PageTreeDTO
            ContentNode.FolderNode(srcKey = blogFolderKey, lastUpdated = now)
        )

        val rootPage = ContentNode.PageNode(
            srcKey = "$domain/sources/pages/about.md",
            lastUpdated = now,
            title = "About",
            templateKey = "sources/templates/page.hbs",
            slug = "about.html",
            isRoot = false,
            attributes = emptyMap(),
            sections = mapOf("main" to "About body"),
            parent = rootFolderKey
        )
        val nestedPage = ContentNode.PageNode(
            srcKey = "$domain/sources/pages/blog/post-1.md",
            lastUpdated = now,
            title = "Post 1",
            templateKey = "sources/templates/page.hbs",
            slug = "post-1.html",
            isRoot = false,
            attributes = emptyMap(),
            sections = mapOf("main" to "Post body"),
            parent = blogFolderKey
        )
        val pages = listOf(rootPage, nestedPage)

        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.listAllNodesForProject(domain, SOURCE_TYPE.Folders) } returns folders
            coEvery { mockDynamoDB.listAllNodesForProject(domain, SOURCE_TYPE.Pages) } returns pages
        }

        val controller = PageController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/pages/tree", pathPattern = "/pages/tree", headers = mapOf("cantilever-project-domain" to domain))

        val response = controller.getPageTree(request)
        assertNotNull(response)
        assertEquals(200, response.statusCode)


        val body = response.body
        assertNotNull(body)
        when (body) {
            is org.liamjd.cantilever.api.models.APIResult.Success -> {
                val tree = body.value
                println(Json.encodeToString(PageTreeDTO.serializer(),tree))

                // Root folder key
                assertEquals(rootFolderKey, tree.rootFolder.srcKey)
                // Root should contain the root-level page and the blog folder
                val rootChildren = tree.rootFolder.children
                val rootFiles = rootChildren.filterIsInstance<org.liamjd.cantilever.models.rest.TreeNode.FileNodeDTO>()
                val rootFolders = rootChildren.filterIsInstance<org.liamjd.cantilever.models.rest.TreeNode.FolderNodeDTO>()
                kotlin.test.assertTrue(rootFiles.any { it.srcKey == rootPage.srcKey }, "Root page not found in tree")
                kotlin.test.assertTrue(rootFolders.any { it.srcKey == blogFolderKey }, "Blog folder not found in tree")

                // Blog folder should contain the nested page
                val blogFolder = rootFolders.first { it.srcKey == blogFolderKey }
                val blogFiles = blogFolder.children.filterIsInstance<org.liamjd.cantilever.models.rest.TreeNode.FileNodeDTO>()
                kotlin.test.assertTrue(blogFiles.any { it.srcKey == nestedPage.srcKey }, "Nested page not found under blog folder")
            }
            else -> kotlin.test.fail("Expected Success result but got $body")
        }
    }

    private fun buildRequest(
        path: String,
        pathPattern: String,
        body: String = "",
        headers: Map<String, String> = mapOf("cantilever-project-domain" to "test")
    ): org.liamjd.apiviaduct.routing.Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = body
        apiGatewayProxyRequestEvent.path = path
        apiGatewayProxyRequestEvent.headers = headers
        return org.liamjd.apiviaduct.routing.Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }
}
