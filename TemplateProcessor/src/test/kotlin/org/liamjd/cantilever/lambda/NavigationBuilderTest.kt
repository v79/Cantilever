package org.liamjd.cantilever.lambda

import io.mockk.coEvery
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
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.GetSingleItemOrdering
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@ExtendWith(MockKExtension::class)
internal class NavigationBuilderTest : KoinTest {

    private val mockDynamoDB = mockk<DynamoDBService>()

    val post1 = ContentNode.PostNode(
        srcKey = "sources/posts/jetpack-compose-theming-woes",
        title = "Jetpack Compose Theming Woes",
        templateKey = "sources/templates/post.html.hbs",
        slug = "jetpack-compose-theming-woes",
        date = LocalDate(2022, 9, 15)
    )
    val post1DateString = "2022-09-15"

    // this is the 'middle' post in our test data
    val post2 = ContentNode.PostNode(
        srcKey = "sources/posts/adding-static-file-support",
        title = "Adding static file support",
        templateKey = "sources/templates/post.html.hbs",
        slug = "adding-static-file-support",
        date = LocalDate(2023, 9, 18)
    )
    val post2DateString = "2023-09-18"
    val post3 = ContentNode.PostNode(
        srcKey = "sources/posts/migrating-to-kotlin-2.0",
        title = "Migrating to Kotlin 2.0",
        templateKey = "sources/templates/post.html.hbs",
        slug = "migrating-to-kotlin-2.0",
        date = LocalDate(2023, 10, 4)
    )
    val post3DateString = "2023-10-04"

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
        declareMock<DynamoDBService> {
            // Get the specific nodes
            coEvery {
                mockDynamoDB.getContentNode(
                    post1.srcKey,
                    "test-domain",
                    SOURCE_TYPE.Posts,
                )
            } returns post1
            coEvery {
                mockDynamoDB.getContentNode(
                    post2.srcKey,
                    "test-domain",
                    SOURCE_TYPE.Posts,
                )
            } returns post2
            coEvery {
                mockDynamoDB.getContentNode(
                    post3.srcKey,
                    "test-domain",
                    SOURCE_TYPE.Posts,
                )
            } returns post3

            // Get the previous post before post2, by date (post1)
            coEvery {
                mockDynamoDB.getKeyListFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    "date" to post2DateString,
                    "<",
                    1,
                    true
                )
            } returns listOf(post1.srcKey)

            // Get the next post after post2, by date (post3)
            coEvery {
                mockDynamoDB.getKeyListFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    "date" to post2DateString,
                    ">",
                    1,
                    true
                )
            } returns listOf(post3.srcKey)

            // Get the previous post before post1, by date (null)
            coEvery {
                mockDynamoDB.getKeyListFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    "date" to post1DateString,
                    "<",
                    1,
                    true
                )
            } returns emptyList()

            // Get the next post after post1, by date (post2)
            coEvery {
                mockDynamoDB.getKeyListFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    "date" to post1DateString,
                    ">",
                    1,
                    true
                )
            } returns listOf(post2.srcKey)

            // Get the prev post after post3, by date (post2)
            coEvery {
                mockDynamoDB.getKeyListFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    "date" to post3DateString,
                    "<",
                    1,
                    true
                )
            } returns listOf(post2.srcKey)

            // Get the next post after post3, by date (null)
            coEvery {
                mockDynamoDB.getKeyListFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    "date" to post3DateString,
                    ">",
                    1,
                    true
                )
            } returns emptyList()

            // Get the first post in the list (post1)
            coEvery {
                mockDynamoDB.getFirstOrLastKeyFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    GetSingleItemOrdering.FIRST
                )
            } returns post1.srcKey

            // Get the last post in the list (post3)
            coEvery {
                mockDynamoDB.getFirstOrLastKeyFromLSI(
                    "test-domain",
                    SOURCE_TYPE.Posts,
                    "Type-Date",
                    GetSingleItemOrdering.LAST
                )
            } returns post3.srcKey


        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `build complete navigation map when given the middle post in the list`() {
        // Setup
        coEvery {
            mockDynamoDB.getFirstOrLastKeyFromLSI(
                "test-domain",
                SOURCE_TYPE.Posts,
                "Type-Date",
                GetSingleItemOrdering.FIRST
            )
        } returns post1.srcKey
        coEvery {
            mockDynamoDB.getFirstOrLastKeyFromLSI(
                "test-domain",
                SOURCE_TYPE.Posts,
                "Type-Date",
                GetSingleItemOrdering.LAST
            )
        } returns post3.srcKey

        // Execute
        val builder = NavigationBuilder(mockDynamoDB, "test-domain")
        val nav = builder.getPostNavigationObjects(post2)

        // Verify
        assertNotNull(nav) {
            assertNotNull(nav["@prev"]) {
                assertEquals("Jetpack Compose Theming Woes", it.title)
            }
            assertNotNull(nav["@next"]) {
                assertEquals("Migrating to Kotlin 2.0", it.title)
            }
            assertNotNull(nav["@first"])
            assertNotNull(nav["@last"])
        }
    }

    @Test
    fun `should return null for previous post when already at the first`() {
        // Setup

        // Execute
        val builder = NavigationBuilder(mockDynamoDB, "test-domain")
        val nav = builder.getPostNavigationObjects(post1)

        // Verify
        assertNotNull(nav) {
            assertNull(nav["@prev"])
            assertNotNull(nav["@last"])
            assertNotNull(nav["@first"]) {
                assertEquals(post1.srcKey, nav["@first"]?.srcKey)
                assertEquals(post1.title, it.title)
            }
            assertNotNull(nav["@next"]) {
                assertEquals(post2.title, it.title)
            }
        }
    }

    @Test
    fun `should return null for previous post when already at the last`() {
        // Setup

        // Execute
        val builder = NavigationBuilder(mockDynamoDB, "test-domain")
        val nav = builder.getPostNavigationObjects(post3)

        // Verify
        assertNotNull(nav) {
            assertNotNull(nav["@prev"]) {
                assertEquals(post2.title, it.title)
            }
            assertNotNull(nav["@last"])
            assertNotNull(nav["@first"]) {
                assertEquals(post1.title, it.title)
            }
            assertNull(nav["@next"])
        }
    }

}

