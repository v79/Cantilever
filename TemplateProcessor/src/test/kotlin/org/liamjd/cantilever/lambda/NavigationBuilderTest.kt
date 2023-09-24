package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.PostMetadata
import org.liamjd.cantilever.services.S3Service
import kotlin.test.*

internal class NavigationBuilderTest {

    private val mockLogger = mockk<LambdaLogger>()
    private val mockS3 = mockk<S3Service>()

    private val sourceBucket = "sourceBucket"
    private val postListJson: String = buildFullJson()
    private val onlyTwoPosts: String = buildJsonWithOnlyTwo()

    @BeforeTest
    fun initTests() {
        every { mockLogger.log(any<String>()) } just runs
    }

    @Test
    fun `build complete navigation map when given the middle post in the list`() {
        every { mockS3.objectExists(S3_KEY.postsKey, sourceBucket) } returns true
        every { mockS3.getObjectAsString(S3_KEY.postsKey, sourceBucket) } returns postListJson

        // this is the 'middle' post in our test data
        val currentPost = PostMetadata(
            title = "Adding static file support",
            template = "post",
            slug = "adding-static-file-support",
            date = LocalDate(2023, 9, 18),
            lastModified = Clock.System.now()
        )
        with(mockLogger) {
            val builder = NavigationBuilder(mockS3)
            val nav = builder.getPostNavigationObjects(currentPost, sourceBucket)
            assertNotNull(nav) {
                assertNotNull(nav["@prev"]) {
                    assertEquals("Jetpack Compose Theming Woes", it.title)
                }
                assertNotNull(nav["@next"]) {
                    assertEquals("DELETE-ME", it.title)
                }
                assertNotNull(nav["@first"])
                assertNotNull(nav["@last"])
            }
        }
    }

    @Test
    fun `should return null for previous post when already at the first`() {
        every { mockS3.objectExists(S3_KEY.postsKey, sourceBucket) } returns true
        every { mockS3.getObjectAsString(S3_KEY.postsKey, sourceBucket) } returns onlyTwoPosts

        // this is the 'first' post in our test data
        val currentPost = PostMetadata(
            title = "Adding static file support",
            template = "post",
            slug = "adding-static-file-support",
            date = LocalDate(2023, 9, 18),
            lastModified = Clock.System.now()
        )
        with(mockLogger) {
            val builder = NavigationBuilder(mockS3)
            val nav = builder.getPostNavigationObjects(currentPost, sourceBucket)
            assertNotNull(nav) {
                assertNull(nav["@prev"])
                assertNotNull(nav["@last"])
                assertNotNull(nav["@first"]) {
                    assertEquals("Adding static file support", it.title)
                }
                assertNotNull(nav["@next"]) {
                    assertEquals("DELETE-ME", it.title)
                }
            }
        }
    }

    @Test
    fun `should return null for previous post when already at the last`() {
        every { mockS3.objectExists(S3_KEY.postsKey, sourceBucket) } returns true
        every { mockS3.getObjectAsString(S3_KEY.postsKey, sourceBucket) } returns onlyTwoPosts

        // this is the 'last' post in our test data
        val currentPost = PostMetadata(
            title = "DELETE-ME",
            template = "post",
            slug = "-posts-delete-me",
            date = LocalDate(2023, 9, 22),
            lastModified = Clock.System.now()
        )
        with(mockLogger) {
            val builder = NavigationBuilder(mockS3)
            val nav = builder.getPostNavigationObjects(currentPost, sourceBucket)
            assertNotNull(nav) {
                assertNotNull(nav["@prev"]) {
                    assertEquals("Adding static file support", it.title)
                }
                assertNotNull(nav["@last"])
                assertNotNull(nav["@first"]) {
                    assertEquals("Adding static file support", it.title)
                }
                assertNull(nav["@next"])
            }
        }
    }

    @Test
    fun `returns empty map if posts json not found`() {
        every { mockS3.objectExists(S3_KEY.postsKey, sourceBucket) } returns false

        // this is the 'first' post in our test data
        val currentPost = PostMetadata(
            title = "Adding static file support",
            template = "post",
            slug = "adding-static-file-support",
            date = LocalDate(2023, 9, 18),
            lastModified = Clock.System.now()
        )
        with(mockLogger) {
            val builder = NavigationBuilder(mockS3)
            val nav = builder.getPostNavigationObjects(currentPost, sourceBucket)
            assertNotNull(nav) {
               assertEquals(0,it.size)
            }
        }
    }

    private fun buildFullJson(): String = """
        {
  "count": 3,
  "lastUpdated": "2023-09-22T17:48:58.939673107Z",
  "posts": [
    {
      "title": "DELETE-ME",
      "srcKey": "sources/posts/DELETE-ME.md",
      "url": "-posts-delete-me",
      "date": "2023-09-22",
      "lastUpdated": "2023-09-22T17:48:58.219960338Z",
      "templateKey": "sources/templates/post.html.hbs"
    },
    {
      "title": "Adding static file support",
      "srcKey": "sources/posts/adding-static-file-support.md",
      "url": "adding-static-file-support",
      "date": "2023-09-18",
      "lastUpdated": "2023-09-22T17:48:58.266828217Z",
      "templateKey": "sources/templates/post.html.hbs"
    },
    {
      "title": "Jetpack Compose Theming Woes",
      "srcKey": "sources/posts/corbel-authentication-and-ui.md",
      "url": "corbel-authentication-and-ui",
      "date": "2023-09-10",
      "lastUpdated": "2023-09-22T17:48:58.354528956Z",
      "templateKey": "sources/templates/post.html.hbs"
    }
    ]
    }
    """.trimIndent()

    private fun buildJsonWithOnlyTwo(): String = """
        {
  "count": 2,
  "lastUpdated": "2023-09-22T17:48:58.939673107Z",
  "posts": [
    {
      "title": "DELETE-ME",
      "srcKey": "sources/posts/DELETE-ME.md",
      "url": "-posts-delete-me",
      "date": "2023-09-22",
      "lastUpdated": "2023-09-22T17:48:58.219960338Z",
      "templateKey": "sources/templates/post.html.hbs"
    },
    {
      "title": "Adding static file support",
      "srcKey": "sources/posts/adding-static-file-support.md",
      "url": "adding-static-file-support",
      "date": "2023-09-18",
      "lastUpdated": "2023-09-22T17:48:58.266828217Z",
      "templateKey": "sources/templates/post.html.hbs"
    }
    ]
    }
    """.trimIndent()
}