package org.liamjd.cantilever.models

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.models.ContentMetaDataBuilder.PostBuilder

class PostBuilderTest {

    @Test
    fun `buildFromSourceString with valid YAML returns PostNode`() {
        val sourceString = """
        title: Test Post
        templateKey: templates/blog
        date: 2025-09-23
        slug: test-post
        attributes:
          tag: kotlin
        """
        val srcKey = "sources/posts/test-post.md"

        val result = PostBuilder.buildFromSourceString(sourceString, srcKey)

        assertNotNull(result)
        assertTrue(result is ContentNode.PostNode)
        assertEquals("Test Post", result.title)
        assertEquals("templates/blog", result.templateKey)
        assertEquals(LocalDate.parse("2025-09-23"), result.date)
        assertEquals("test-post", result.slug)
        assertEquals(mapOf("tag" to "kotlin"), result.attributes)
        assertEquals(srcKey, result.srcKey)
    }

    @Test
    fun `buildFromSourceString with missing fields throws Exception`() {
        val sourceString = """
        title: Test Post
        templateKey: templates/blog
        """
        val srcKey = "sources/posts/test-post.md"

        assertThrows(Exception::class.java) {
            PostBuilder.buildFromSourceString(sourceString, srcKey)
        }
    }

    @Test
    fun `buildWithoutYaml creates PostNode with default values`() {
        val srcKey = "sources/posts/test-post.md"

        val result = PostBuilder.buildWithoutYaml(srcKey)

        assertNotNull(result)
        assertTrue(result is ContentNode.PostNode)
        assertEquals("test-post", result.title)
        assertEquals(S3_KEY.defaultPostTemplateKey, result.templateKey)
        assertEquals("test-post", result.slug)
        assertEquals(LocalDate.now(), result.date) // Check if date is the current date
        assertEquals(srcKey, result.srcKey)
    }

    @Test
    fun `buildFromSourceString sets srcKey correctly`() {
        val sourceString = """
        title: Test Post
        templateKey: templates/blog
        date: 2025-09-23
        slug: test-post
        attributes:
          tag: kotlin
        """
        val srcKey = "sources/posts/test-post.md"

        val result = PostBuilder.buildFromSourceString(sourceString, srcKey)

        assertEquals(srcKey, result.srcKey)
    }

    @Test
    fun `buildWithoutYaml handles complex srcKey structure`() {
        val srcKey = "sources/posts/subfolder/test-post.md"

        val result = PostBuilder.buildWithoutYaml(srcKey)

        assertNotNull(result)
        assertEquals("test-post", result.title)
        assertEquals("subfolder-test-post", result.slug)
    }
}