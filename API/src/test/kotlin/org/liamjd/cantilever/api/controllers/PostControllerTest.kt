package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
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
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.models.rest.PostListDTO
import org.liamjd.cantilever.repositories.ContentRepository
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
internal class PostControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockContentRepository: ContentRepository by inject()
    private val sourceBucket = "sourceBucket"
    private val generationBucket = "generationBucket"
    private val testDomain = "test.domain.com"

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module {
            single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
            single<ContentRepository> { mockk() }
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
    fun `getPosts returns a list of posts when content tree is loaded successfully`() {
        // Arrange
        val post1 = ContentNode.PostNode(
            srcKey = "sources/posts/post1.md",
            title = "Post 1",
            templateKey = "templates/post.html.hbs",
            date = LocalDate(2023, 7, 15),
            slug = "post-1",
            attributes = mapOf("author" to "Test Author")
        )
        val post2 = ContentNode.PostNode(
            srcKey = "sources/posts/post2.md",
            title = "Post 2",
            templateKey = "templates/post.html.hbs",
            date = LocalDate(2023, 8, 20),
            slug = "post-2",
            attributes = mapOf("author" to "Test Author")
        )
        
        // Create a non-empty content tree
        val mockContentTree = ContentTree()
        mockContentTree.items.add(ContentNode.FolderNode("sources/posts"))
        mockContentTree.items.add(post1)
        mockContentTree.items.add(post2)
        
        // Mock the ContentRepository to return posts
        declareMock<ContentRepository> {
            every { mockContentRepository.getContentTree(testDomain) } returns mockContentTree
            every { mockContentRepository.getPostsInOrder(testDomain, any()) } returns listOf(post1, post2)
            every { mockContentRepository.saveContentTree(testDomain, any()) } returns true
        }
        
        // Mock S3Service for the fallback path
        declareMock<S3Service> {
            every { mockS3.objectExists("$testDomain/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("$testDomain/metadata.json", generationBucket) } returns "{}"
            every { mockS3.putObjectAsString(any(), any(), any(), any()) } returns 100
        }
        
        // Create the controller and request
        val controller = PostController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/posts", pathPattern = "/posts", domain = testDomain)
        
        // Act
        val response = controller.getPosts(request)
        
        // Assert
        assertNotNull(response)
        assertEquals(200, response.statusCode)
        assertNotNull(response.body) {
            assertTrue(it is APIResult.Success, "Expected Success but got ${it::class.simpleName}")
            val success = it as APIResult.Success
            val postList = success.value as PostListDTO
            assertEquals(2, postList.count)
            assertEquals(2, postList.posts.size)
            // Posts should be sorted by date in descending order
            assertEquals("Post 2", postList.posts[0].title)
            assertEquals("Post 1", postList.posts[1].title)
        }
    }

    @Test
    fun `getPosts returns 404 when content tree cannot be found`() {
        // Arrange
        declareMock<ContentRepository> {
            every { mockContentRepository.getContentTree(testDomain) } returns ContentTree()
            every { mockContentRepository.getPostsInOrder(testDomain, any()) } returns emptyList()
        }

        // Mock S3Service for the fallback path
        declareMock<S3Service> {
            every { mockS3.objectExists("$testDomain/metadata.json", generationBucket) } returns false
        }

        // Create the controller and request
        val controller = PostController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/posts", pathPattern = "/posts", domain = testDomain)
        
        // Act
        val response = controller.getPosts(request)

        // Assert
        assertNotNull(response)
        assertEquals(404, response.statusCode)
        assertTrue(response.body is APIResult.Error)
    }

    @Test
    fun `getPosts returns 500 when posts list is empty`() {
        // Arrange
        val mockContentTree = ContentTree()
        mockContentTree.items.add(ContentNode.FolderNode("sources/posts"))
        
        // Mock the ContentRepository
        declareMock<ContentRepository> {
            every { mockContentRepository.getContentTree(testDomain) } returns mockContentTree
            every { mockContentRepository.getPostsInOrder(testDomain, any()) } returns emptyList()
            every { mockContentRepository.saveContentTree(any(), any()) } returns true
        }
        
        // Mock S3Service for the fallback path
        declareMock<S3Service> {
            every { mockS3.objectExists("$testDomain/metadata.json", generationBucket) } returns true
            every { mockS3.getObjectAsString("$testDomain/metadata.json", generationBucket) } returns "{}"
        }

        // Create the controller and request
        val controller = PostController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/posts", pathPattern = "/posts", domain = testDomain)
        
        // Act
        val response = controller.getPosts(request)

        // Assert
        assertNotNull(response)
        assertEquals(500, response.statusCode)
        assertTrue(response.body is APIResult.Error)
    }

    @Test
    fun `getPosts returns 500 when cantilever-project-domain header is missing`() {
        // Create the controller and request without domain header
        val controller = PostController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/posts", pathPattern = "/posts", includeDomainHeader = false)
        
        // Act
        val response = controller.getPosts(request)

        // Assert
        assertNotNull(response)
        assertEquals(500, response.statusCode)
        assertTrue(response.body is APIResult.Error)
        val error = response.body as APIResult.Error
        assertTrue(error.statusText.contains("null"))
    }
    

    /**
     * Utility function to build the fake request object
     */
    private fun buildRequest(
        path: String, 
        pathPattern: String, 
        body: String = "",
        domain: String = "test",
        includeDomainHeader: Boolean = true
    ): Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = body
        apiGatewayProxyRequestEvent.path = path
        
        val headers = mutableMapOf<String, String>()
        if (includeDomainHeader) {
            headers["cantilever-project-domain"] = domain
        }
        
        apiGatewayProxyRequestEvent.headers = headers
        return Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }
}