package org.liamjd.cantilevers.models

/**
 * This is an experiment
 */

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.models.FolderNotEmptyException

class ContentTreeTest {

    private val pretty = Json { prettyPrint = true }

    private val latestPost = ContentNode.PostNode(
        srcKey = "sources/posts/2023/11/11/latest.md",
        title = "Latest Post",
        templateKey = "post",
        slug = "/posts/2023/11/11/latest",
        date = LocalDate(2023, 11, 11),
        attributes = mapOf("author" to "Liam", "tags" to "test"),
    )
    private val earliest = ContentNode.PostNode(
        srcKey = "sources/posts/2023/11/01/earliest.md",
        title = "Earliest Post",
        templateKey = "post",
        slug = "/posts/2023/01/11/earliest",
        date = LocalDate(2023, 1, 11),
        attributes = mapOf("author" to "Mike", "tags" to "test"),
    )
    private val middle = ContentNode.PostNode(
        srcKey = "sources/posts/2023/04/11/middle.md",
        title = "Middle Post",
        templateKey = "post",
        slug = "/posts/2023/04/11/middle",
        date = LocalDate(2023, 4, 11),
        attributes = mapOf("author" to "Mike", "tags" to "test"),
    )
    private val bioFolder = ContentNode.FolderNode(
        srcKey = "sources/pages/biography",
    )
    private val bookFolder = ContentNode.FolderNode(
        srcKey = "sources/pages/books",
    )
    private val bioPage = ContentNode.PageNode(
        srcKey = "sources/pages/biography/bio.md",
        title = "Biography",
        templateKey = "page",
        slug = "/biography/index",
        isRoot = true,
        attributes = mapOf("author" to "Liam", "tags" to "test"),
        sections = mapOf("bio" to "This is my biography")
    ).apply { parent = bioFolder.srcKey }
    private val booksPage = ContentNode.PageNode(
        srcKey = "sources/pages/books/books.md",
        title = "Books",
        templateKey = "sources/templates/myTemplate.hbs",
        slug = "/books/index",
        isRoot = true,
        attributes = mapOf("author" to "Liam", "tags" to "test"),
        sections = mapOf("books" to "This is my books page")
    ).apply { parent = bookFolder.srcKey }
    private val favouriteBook = ContentNode.PageNode(
        srcKey = "sources/pages/books/favourite.md",
        title = "Favourite Book",
        templateKey = "page",
        slug = "/books/favourite",
        isRoot = false,
        attributes = mapOf("author" to "Liam", "tags" to "test"),
        sections = mapOf("book" to "This is my favourite book")
    ).apply { parent = bookFolder.srcKey }

    @Test
    fun `show posts as json`() {
        val items = ContentTree()
        items.insertAll(listOf(latestPost, earliest, middle, bioFolder, bookFolder, bioPage, booksPage, favouriteBook))
        println(pretty.encodeToString(items))
    }

    @Test
    fun `can insert new post in the middle and update prev and next links for all`() {

        val items = ContentTree(
        )

        items.insertPost(latestPost)
        items.insertPost(earliest)
        items.insertPost(middle)

        println(pretty.encodeToString(items.items.filterIsInstance<ContentNode.PostNode>()))

        assertEquals(3, items.items.filterIsInstance<ContentNode.PostNode>().size)
        val last = items.items.find { it.srcKey == latestPost.srcKey } as ContentNode.PostNode
        assertNull(last.next, "last post should have no next")
        val middle = items.items.find { it.srcKey == middle.srcKey } as ContentNode.PostNode
        assertNotNull(middle.prev)
        assertNotNull(middle.next)
        val first = items.items.find { it.srcKey == earliest.srcKey } as ContentNode.PostNode
        assertNull(first.prev, "first post should have no prev")
        assertNotNull(first.next)
        assertEquals(middle.srcKey, first.next)
    }

    @Test
    fun `deleting a post updates prev and next links`() {
        val items = ContentTree(
        )
        items.insertPost(latestPost)
        items.insertPost(earliest)
        items.insertPost(middle)

        items.deletePost(earliest)
        println(pretty.encodeToString(items.items.filterIsInstance<ContentNode.PostNode>()))
        val first = items.items.find { it.srcKey == middle.srcKey } as ContentNode.PostNode
        assertNull(first.prev, "first post should have no prev")
        val second = items.items.find { it.srcKey == latestPost.srcKey } as ContentNode.PostNode
        assertNull(second.next, "second post should have no next")
    }

    @Test
    fun `insert then delete a page`() {
        val items = ContentTree()
        items.insertPage(bioPage)
        assertEquals(1, items.items.count())

        items.deletePage(bioPage)
        assertEquals(0, items.items.count())
    }

    @Test
    fun `insert page and folder`() {
        val items = ContentTree()
        items.insertFolder(bookFolder)
        items.insertPage(booksPage, bookFolder)
        items.insertPage(favouriteBook, bookFolder)
        assertEquals(3, items.items.count())
        val folder = items.items.find { it.srcKey == bookFolder.srcKey } as ContentNode.FolderNode
        assertEquals(2, folder.count, "folder should have 2 children")
        assertEquals(booksPage.srcKey, folder.indexPage, "book folder should have index page")
        assertEquals(bookFolder.srcKey, booksPage.parent, "books page should have parent folder")
    }

    @Test
    fun `can get next and previous from just srcKey`() {
        val items = ContentTree()
        items.insertPost(latestPost)
        items.insertPost(earliest)
        items.insertPost(middle)

        val next = items.getNextPost(middle.srcKey)
        assertNotNull(next)
        val prev = items.getPrevPost(middle.srcKey)
        assertNotNull(prev)
    }

    @Test
    fun `updating a page does not change the parent folder`() {
        val items = ContentTree()
        items.insertFolder(bioFolder)
        items.insertPage(bioPage, bioFolder)
        items.insertPage(booksPage)

        val updatedBioPage = bioPage.copy(title = "Updated Bio")
        items.updatePage(updatedBioPage)

        val folder = items.items.find { it.srcKey == bioFolder.srcKey } as ContentNode.FolderNode
        assertEquals(1, folder.count)
    }

    @Test
    fun `updating a post date updates the prev and next links`() {
        val items = ContentTree()
        items.insertPost(latestPost)
        items.insertPost(earliest)
        items.insertPost(middle)

        val updatedMiddle = middle.copy(date = LocalDate(2024, 1, 11)).apply { srcKey = middle.srcKey }
        items.updatePost(updatedMiddle)

        val first = items.items.find { it.srcKey == earliest.srcKey } as ContentNode.PostNode
        assertNull(first.prev, "first post should have no prev")
        val second = items.items.find { it.srcKey == latestPost.srcKey } as ContentNode.PostNode
        assertNotNull(second.next, updatedMiddle.srcKey)
        val third = items.items.find { it.srcKey == updatedMiddle.srcKey } as ContentNode.PostNode
        assertNull(third.next, "updated middle is now at the end and should not have next")
        assertEquals(second.srcKey, updatedMiddle.prev)
    }

    @Test
    fun `can move a page to a different folder`() {
        val items = ContentTree()
        items.insertFolder(bioFolder)
        items.insertFolder(bookFolder)
        items.insertPage(bioPage, bioFolder)
        items.insertPage(booksPage, bookFolder)
        items.insertPage(favouriteBook, bookFolder)

        items.reparentPage(favouriteBook, bioFolder)

        val bioFolder = items.items.find { it.srcKey == bioFolder.srcKey } as ContentNode.FolderNode
        assertEquals(2, bioFolder.count)
        val bookFolder = items.items.find { it.srcKey == bookFolder.srcKey } as ContentNode.FolderNode
        bookFolder.children.forEach { println(it) }
        assertEquals(1, bookFolder.count)
    }

    @Test
    fun `throw exception when deleting a non-empty folder`() {
        val items = ContentTree()
        items.insertFolder(bioFolder)
        items.insertPage(bioPage, bioFolder)

        assertThrows(FolderNotEmptyException::class.java) {
            items.deleteFolder(bioFolder)
        }
    }

    @Test
    fun `can delete a folder`() {
        val items = ContentTree()
        items.insertFolder(bioFolder)
        items.insertPage(bioPage, bioFolder)

        items.deletePage(bioPage)
        items.deleteFolder(bioFolder)

        assertEquals(0, items.items.count())
    }

    @Test
    fun `can insert and delete a template`() {
        val items = ContentTree()
        val template = ContentNode.TemplateNode(
            srcKey = "sources/templates/myTemplate.hbs",
            title = "My Template",
            sections = listOf("header", "footer")
        )
        items.insertTemplate(template)
        items.insertPage(booksPage)
        assertEquals(1, items.templates.count())
        assertEquals(1, items.getPagesForTemplate(template.srcKey).count())
        items.deleteTemplate(template)
        assertEquals(0, items.templates.count())
    }
}
