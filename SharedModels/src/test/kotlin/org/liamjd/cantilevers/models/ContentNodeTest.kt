package org.liamjd.cantilevers.models

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode

class ContentNodeTest {

    @Test
    fun `generate URL for post`() {
        val post = ContentNode.PostNode(
            srcKey = "sources/posts/2023/11/11/latest.md",
            title = "Latest Post",
            templateKey = "post",
            slug = "latest",
            date = LocalDate.parse("2023-11-11"),
            attributes = mapOf("author" to "Liam", "tags" to "test"),
        )
        val url = post.url
        assertEquals("posts/2023/11/latest", url)
    }

    @Test
    fun `generate URL for page`() {
        val page = ContentNode.PageNode(
            srcKey = "sources/pages/bio.md",
            title = "Biography",
            templateKey = "page",
            slug = "biography",
            isRoot = false,
            attributes = mapOf("author" to "Liam", "tags" to "test"),
            sections = mapOf("bio" to "This is my biography")
        ).apply { parent = "sources/pages/" }
        val url = page.url
        assertEquals("biography", url)
    }

    @Test
    fun `generate indexhtml for root page`() {
        val page = ContentNode.PageNode(
            srcKey = "sources/pages/bio.md",
            title = "Biography",
            templateKey = "page",
            slug = "biography",
            isRoot = true,
            attributes = mapOf("author" to "Liam", "tags" to "test"),
            sections = mapOf("bio" to "This is my biography")
        ).apply { parent = "sources/pages/" }
        val url = page.url
        assertEquals("index.html", url)
    }

    @Test
    fun `generate url for nested page`() {
        val page = ContentNode.PageNode(
            srcKey = "sources/pages/books/favourite-books.md",
            title = "Books",
            templateKey = "page",
            slug = "favourite-books",
            isRoot = false,
            attributes = mapOf("author" to "Liam", "tags" to "test"),
            sections = mapOf("bio" to "This is my biography")
        ).apply { parent = "sources/pages/books/" }
        val url = page.url
        assertEquals("books/favourite-books", url)
    }

    @Test
    fun `generate url for root nested page`() {
        val page = ContentNode.PageNode(
            srcKey = "sources/pages/bio/about-me.md",
            title = "About me",
            templateKey = "sources/templates/about.html.hbs",
            slug = "bio/about-me",
            isRoot = true,
            attributes = mapOf("author" to "Liam", "tags" to "test"),
            sections = mapOf("bio" to "This is my biography")
        ).apply { parent = "sources/pages/bio/" }
        val url = page.url
        assertEquals("bio/index.html", url)
    }

    @Test
    fun `build index node with no slug from source`() {
        val source = """
            ---
            title: "Home"
            templateKey: "sources/templates/index.html.hbs"
            isRoot: true
            --- #body
            Home page content here
        """.trimIndent()

        val indexNode = ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString(source, "sources/pages/index.md")
        indexNode.parent = "sources/pages/"
        assertEquals("index.html", indexNode.url)
    }
}