package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.openapi.APISchema

/**
 * TODO: replace this with a metadata interface with implementations for Post, Page, Template
 */
/**
 * Wrapper around the list of all posts. This represents posts.json
 * @property count total number of posts
 * @property lastUpdated last updated date/time for the posts.json file
 * @property posts list of meta-data objects for each post
 */
@Serializable
data class PostList(val count: Int = 0, val lastUpdated: Instant, val posts: List<PostMeta>)

/**
 * Wrapper around the list of all templates. This represents templates.json
 * @property count total number of templates
 * @property lastUpdated last updated date/time for the templates.json file
 * @property templates list of all meta-data objects for each template
 */
@Serializable
@APISchema
data class TemplateList(val count: Int = 0, val lastUpdated: Instant, val templates: List<Template>)

/**
 * PostMeta is a summary class representing an individual Post. It does not contain the contents
 * @property title user-provided title
 * @property srcKey the full S3 key for the post, must be unique
 * @property url the calculated URL for the post, this is initialized as the title, sanitised for the web ("slug"), but can be overridden by the user when first creating the post
 * @property date user-provided date for the post; used in sorting
 * @property lastUpdated internal property updated whenever the post is saved
 * @property templateKey the leaf of the S3 key for the template this post is based on (e.g. if the full template key is /sources/templates/myTemplate.hbs then this value will be 'myTemplate'
 */
@Deprecated(message = "Use ContentNode.PostNode instead")
@Serializable
data class PostMeta(
    val title: String,
    val srcKey: String,
    val url: String,
    val date: LocalDate,
    val lastUpdated: Instant = Clock.System.now(),
    val templateKey: String
)

/**
 * Represents a Handlebars template file. It does not contain the content. There is no yaml front-matter, and hence no metadata, for Templates.
 * @property srcKey the full S3 key for this template ('sources/templates/myTemplate.hbs'). Note that [PageMeta] and [PostMeta] only refer to the leaf of this, i.e. 'myTemplate'
 * @property metadata the [TemplateMetadata] object for this template, containing name and sections
 * @property lastUpdated internal property updated whenever the template is saved.
 */
@Serializable
@APISchema
data class Template(val srcKey: String, val lastUpdated: Instant = Clock.System.now(), val metadata: TemplateMetadata)

/**
 * Represents the frontmatter metadata for a handlebars template file
 * @property name user-friendly name of the template
 * @property sections list of custom section names for the template
 */
@Serializable
@APISchema
data class TemplateMetadata(val name: String, val sections: List<String>? = emptyList())
