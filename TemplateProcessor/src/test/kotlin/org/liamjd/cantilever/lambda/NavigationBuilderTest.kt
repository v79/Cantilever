package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock
import org.liamjd.cantilever.common.EnvironmentProvider
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@ExtendWith(MockKExtension::class)
internal class NavigationBuilderTest : KoinTest {

    private val mockLogger = mockk<LambdaLogger>()
    private val mockDynamoDB = mockk<DynamoDBService>()
    private val mockS3 = mockk<S3Service>()
    private val sourceBucket = "sourceBucket"

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

        declareMock<EnvironmentProvider> {

        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `build complete navigation map when given the middle post in the list`() {

        // this is the 'middle' post in our test data
        val currentPost = ContentNode.PostNode(
            srcKey = "sources/posts/adding-static-file-support",
            title = "Adding static file support",
            templateKey = "sources/templates/post.html.hbs",
            slug = "adding-static-file-support",
            date = LocalDate(2023, 9, 18)
        )
        val builder = NavigationBuilder(mockDynamoDB, "test-domain")
        val nav = builder.getPostNavigationObjects(currentPost)
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

    @Test
    fun `should return null for previous post when already at the first`() {

        // this is the 'first' post in our test data
        val currentPost = ContentNode.PostNode(
            srcKey = "sources/posts/adding-static-file-support",
            title = "Adding static file support",
            templateKey = "sources/templates/post.html.hbs",
            slug = "adding-static-file-support",
            date = LocalDate(2023, 9, 18)
        )
        val builder = NavigationBuilder(mockDynamoDB, "test-domain")
        val nav = builder.getPostNavigationObjects(currentPost)
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

    @Test
    fun `should return null for previous post when already at the last`() {

        // this is the 'last' post in our test data
        val currentPost = ContentNode.PostNode(
            srcKey = "sources/posts/adding-static-file-support",
            title = "Adding static file support",
            templateKey = "sources/templates/post.html.hbs",
            slug = "adding-static-file-support",
            date = LocalDate(2023, 9, 18)
        )
        val builder = NavigationBuilder(mockDynamoDB, "test-domain")
        val nav = builder.getPostNavigationObjects(currentPost)
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

    @Test
    fun `returns empty map if posts json not found`() {

        // this is the 'first' post in our test data
        val currentPost = ContentNode.PostNode(
            srcKey = "sources/posts/adding-static-file-support",
            title = "Adding static file support",
            templateKey = "sources/templates/post.html.hbs",
            slug = "adding-static-file-support",
            date = LocalDate(2023, 9, 18)
        )
        val builder = NavigationBuilder(mockDynamoDB, "test-domain")
        val nav = builder.getPostNavigationObjects(currentPost)
        assertNotNull(nav) {
            assertEquals(0, it.size)
        }
    }
}

