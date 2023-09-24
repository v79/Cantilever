package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class NavigationBuilderTest {

    private val mockLogger = mockk<LambdaLogger>()

    private val postListJson: String = """
        {
  "count": 34,
  "lastUpdated": "2023-09-22T17:48:58.939673107Z",
  "posts": [
    {
      "title": "sources/posts/DELETE-ME",
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

    @BeforeTest
    fun initTests() {
        every { mockLogger.log(any<String>()) } just runs
    }

    @Test
    fun `should return previous post`() {


    }
}