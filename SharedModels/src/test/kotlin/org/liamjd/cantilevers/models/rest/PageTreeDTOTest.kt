package org.liamjd.cantilevers.models.rest

import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.PageTreeDTO
import org.liamjd.cantilever.models.rest.TreeNode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull


class PageTreeDTOTest {

    @Test
    fun `should create page tree DTO`() {
        // setup
        val allNodes = mutableListOf<ContentNode>()
        allNodes.addAll(pageNodes)
        allNodes.addAll(folderNodes)

        // execute
        val pageTreeDTO = PageTreeDTO(TreeNode.FolderNodeDTO("sources/pages", Clock.System.now()))
        pageTreeDTO.buildTreeFromPagesAndFolders(folderNodes, pageNodes)

        // verify
        assertNotNull(pageTreeDTO)
        assertEquals("sources/pages", pageTreeDTO.rootFolder.srcKey)
        // 4 children - about.md, contact.md, biography folder, index.md
        assertEquals(4, pageTreeDTO.rootFolder.count)
        print(pageTreeDTO.printTree())
        for (children in pageTreeDTO.rootFolder.children) {
            when (children) {
                is TreeNode.FolderNodeDTO -> {
                    // biography folder
                    assertEquals("sources/pages/biography", children.srcKey)
                    assertEquals(2, children.count) // bio.md and career folder
                    for (bioChildren in children.children) {
                        when (bioChildren) {
                            is TreeNode.FolderNodeDTO -> {
                                // career folder
                                assertEquals("sources/pages/biography/career", bioChildren.srcKey)
                                assertEquals(1, bioChildren.count) // career.md
                            }

                            is TreeNode.FileNodeDTO -> {
                                // bio.md
                                assertEquals("sources/pages/biography/bio.md", bioChildren.srcKey)
                            }
                        }
                    }
                }

                is TreeNode.FileNodeDTO -> {
                    // about.md, contact.md, index.md
                    assertNotEquals("sources/pages/biography/bio.md", children.srcKey)
                }
            }
        }
    }


    @Test
    fun `can serialize and deserialize PageTreeDTO`() {
        // setup
        val json = Json { prettyPrint = true }
        val allNodes = mutableListOf<ContentNode>()
        allNodes.addAll(pageNodes)
        allNodes.addAll(folderNodes)

        val pageTreeDTO = PageTreeDTO(TreeNode.FolderNodeDTO("sources/pages", Clock.System.now()))
        pageTreeDTO.buildTreeFromPagesAndFolders(folderNodes, pageNodes)

        // execute
        val jsonString = json.encodeToString(pageTreeDTO)
        val deserialized = json.decodeFromString<PageTreeDTO>(jsonString)

        // verify
        println(jsonString)
        assertNotNull(deserialized)
        assertEquals(pageTreeDTO.rootFolder.srcKey, deserialized.rootFolder.srcKey)
        assertEquals(pageTreeDTO.rootFolder.count, deserialized.rootFolder.count)
        assertEquals(pageTreeDTO.printTree(), deserialized.printTree())
    }
}


val pageNodes = listOf(
    ContentNode.PageNode(
        title = "Home",
        srcKey = "sources/pages/index.md",
        templateKey = "sources/templates/home.html.hbs",
        slug = "",
        attributes = emptyMap(),
        sections = emptyMap(),
        isRoot = true
    ),
    ContentNode.PageNode(
        title = "About",
        srcKey = "sources/pages/about.md",
        templateKey = "sources/templates/page.html.hbs",
        slug = "about",
        attributes = emptyMap(),
        sections = emptyMap(),
        isRoot = false
    ),
    ContentNode.PageNode(
        title = "Contact",
        srcKey = "sources/pages/contact.md",
        templateKey = "sources/templates/page.html.hbs",
        slug = "contact",
        attributes = emptyMap(),
        sections = emptyMap(),
        isRoot = false
    ),
    ContentNode.PageNode(
        title = "Biography",
        srcKey = "sources/pages/biography/bio.md",
        templateKey = "sources/templates/page.html.hbs",
        slug = "biography/bio",
        attributes = emptyMap(),
        sections = emptyMap(),
        isRoot = true
    ),
    ContentNode.PageNode(
        title = "Career",
        srcKey = "sources/pages/biography/career/career.md",
        templateKey = "sources/templates/page.html.hbs",
        slug = "biography/career/career",
        attributes = emptyMap(),
        sections = emptyMap(),
        isRoot = true
    )
)

val folderNodes = listOf(
    ContentNode.FolderNode(
        srcKey = "sources/pages"
    ),
    ContentNode.FolderNode(
        srcKey = "sources/pages/biography"
    ),
    ContentNode.FolderNode(
        srcKey = "sources/pages/biography/career"
    )
)