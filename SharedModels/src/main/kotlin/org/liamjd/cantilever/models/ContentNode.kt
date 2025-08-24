package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.*
import org.liamjd.cantilever.common.S3_KEY
import kotlin.time.ExperimentalTime

/**
 * A SrcKey is a String representation of the S3 bucket object key
 * It is used to uniquely identify a file in the bucket
 **/
typealias SrcKey = String // an S3 bucket object key

/**
 * A ContentNode is a representation of a file in the S3 bucket. It is used to build the ContentTree, which is used to generate the metadata representation of the site
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
/**
 * Sealed class representing different types of content nodes in a content management system.
 * Each subclass represents a specific type of content node with its own properties and behaviors.
 * @property srcKey The source key of the content node. This will be fully qualified for S3 access, e.g. "<domain.com>/sources/pages/folder/page.md"
 * @property lastUpdated The timestamp of the last update to the content node, persisted as milliseconds since epoch.
 * @property url The URL where the content node can be accessed, relative to the site root
 */
sealed class ContentNode {
    abstract val srcKey: SrcKey

    @OptIn(ExperimentalTime::class)
    abstract val lastUpdated: Instant
    abstract val url: String

    /**
     * A folder is a node in the tree which contains other nodes
     * It is not used for the four core 'type' folders (pages, posts, templates, statics)
     * @property children A list of SrcKeys representing the children of the folder
     * @property indexPage The SrcKey of the index page for the folder, if any
     */
    @Serializable
    @SerialName("folder")
    data class FolderNode @OptIn(ExperimentalTime::class) constructor(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
        val children: MutableList<SrcKey> = mutableListOf(),
        var indexPage: SrcKey? = null,
    ) : ContentNode() {
        val count: Int
            get() = children.size

        override val url = srcKey.removePrefix(S3_KEY.pagesPrefix)

        override fun equals(other: Any?): Boolean {
            if (other !is FolderNode) return false
            return srcKey == other.srcKey
        }

        override fun hashCode(): Int {
            var result = srcKey.hashCode()
            result = 31 * result + lastUpdated.hashCode()
            result = 31 * result + children.hashCode()
            result = 31 * result + (indexPage?.hashCode() ?: 0)
            result = 31 * result + url.hashCode()
            result = 31 * result + count
            return result
        }
    }

    /**
     * A page is a node in the tree which represents a page on the site. A page belongs to a Folder and may be the index page for that folder
     * @property title The title of the page, used in the HTML title tag and in navigation
     * @property templateKey The SrcKey of the template used to render the page, e.g. "sources/templates/page.html.hbs" (no domain)
     * @property slug The slug of the page, used in the URL
     * @property isRoot Whether the page is the index page for its parent folder
     * @property attributes A map of additional attributes for the page, from the frontmatter
     * @property sections A map of section names to content, from the frontmatter
     * @property parent The SrcKey of the parent folder
     */
    @Serializable
    @SerialName("page")
    data class PageNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val templateKey: SrcKey,
        val slug: String,
        val isRoot: Boolean,
        val attributes: Map<String, String>,
        val sections: Map<String, String>,
        var parent: SrcKey = ""
    ) : ContentNode() {

        @Transient
        val type: String? =
            null // this is used by the front end to determine if it's a page or a post, but is not needed here

        // I might actually want to generate both index.html and the slug version of the file, but for now let's just do index.html or slug
        override val url: String
            get() {
                // intended url would be www.cantilevers.org/parentFolder/slug
                // but parent looks like wwww.cantilevers.org/sources/pages/parentFolder
                val parentFolder = parent.replaceFirst("/sources/pages", "")
                return if (isRoot) {
                    "${parentFolder}/index.html"
                } else {
                    "$parentFolder/$slug"
                }
            }
    }

    /**
     * A post is a node in the tree which represents a blog post on the site. Posts are not in folders but are linked together in a sequence
     * @property title The title of the post, used in the HTML title tag and in navigation
     * @property templateKey The SrcKey of the template used to render the post, e.g. "sources/templates/post.html.hbs" (no domain)
     * @property date The date of the post, used to order posts and in the URL
     * @property slug The slug of the post, used in the URL
     * @property attributes A map of additional attributes for the post, from the frontmatter
     * @property body The body content of the post, in Markdown format
     * @property next The SrcKey of the next post in the sequence, if any
     * @property prev The SrcKey of the previous post in the sequence, if any
     */
    @Serializable
    @SerialName("post")
    data class PostNode(
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val templateKey: SrcKey,
        val date: LocalDate,
        val slug: String,
        @EncodeDefault
        val attributes: Map<String, String> = emptyMap()
    ) : ContentNode() {

        // I'd like to make this configurable, but let's try a folder structure based on the date
        override val url: String
            get() {
                val year = date.year.toString()
                val month = date.monthNumber.toString().padStart(2, '0')
                return "posts/$year/$month/$slug"
            }

        /**
         * Secondary constructor for when we know the srcKey
         */
        constructor(
            srcKey: String,
            title: String,
            templateKey: String,
            date: LocalDate,
            slug: String,
            attributes: Map<String, String> = emptyMap(),
            lastUpdated: Instant = Clock.System.now(),
        ) : this(
            title = title,
            templateKey = templateKey,
            date = date,
            slug = slug,
            attributes = attributes,
            lastUpdated = lastUpdated,
        ) {
            this.srcKey = srcKey
        }

        /**
         * Secondary constructor to convert a temporary [PostYaml] object into a [PostNode]
         */
        internal constructor(postYaml: PostYaml) : this(
            title = postYaml.title,
            templateKey = postYaml.templateKey,
            date = postYaml.date,
            slug = postYaml.slug,
            attributes = postYaml.attributes
        )

        override lateinit var srcKey: String
        var body: String = ""
        var next: SrcKey? = null
        var prev: SrcKey? = null
    }

    /**
     * A template is a node in the tree which represents a Handlebars template. It is used to generate the final HTML for a page
     * @property title The title of the template, used in the editing interface
     * @property sections A list of section names which the template defines
     * @property body The body content of the template, in Handlebars format
     */
    @Serializable
    @SerialName("template")
    data class TemplateNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val sections: List<String> = emptyList(),
    ) : ContentNode() {
        override val url = "" // irrelevant for templates
        var body: String = ""
    }

    /**
     * A Static is a node in the tree which represents a static file, such as a .css file. It is copied to the generated bucket without modification
     * @property fileType The MIME type of the static file, e.g. "text/css"
     */
    @Serializable
    @SerialName("static")
    data class StaticNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
    ) : ContentNode(
    ) {
        var fileType: String? = null
        override val url = srcKey.removePrefix(S3_KEY.sources) + "/"
    }

    /**
     * An image is a node in the tree which represents an image file. It is copied to the generated bucket without modification
     * @property contentType The MIME type of the image file, e.g. "image/png"
     */
    @Serializable
    @SerialName("image")
    data class ImageNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
    ) : ContentNode(
    ) {
        var contentType: String? = null
        override val url = "" // irrelevant for images, or rather, handled differently because of the image resolutions

        // should I be recording image resolutions here?
    }
}

/**
 * Exception thrown when attempting to delete a folder which contains children
 */
class FolderNotEmptyException(message: String) : Exception(message)

/**
 * This is a utility class because [ContentNode.PostNode] requires the `srcKey` property, which will not exist in the YAML frontmatter for a post.
 * I had hoped to avoid this duplication of classes, but I can't see a way around it because making `srcKey` abstract in [ContentNode] does not work.
 */
@Serializable
internal class PostYaml(
    val title: String,
    val templateKey: String,
    val date: LocalDate,
    val slug: String,
    val attributes: Map<String, String> = emptyMap()
)
