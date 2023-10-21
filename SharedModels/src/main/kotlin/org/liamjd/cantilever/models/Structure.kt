package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Wrapper around the list of all posts. This represents posts.json
 * @property count total number of posts
 * @property lastUpdated last updated date/time for the posts.json file
 * @property posts list of meta-data objects for each post
 */
@Serializable
data class PostList(val count: Int = 0, val lastUpdated: Instant, val posts: List<PostMeta>)

/**
 * Wrapper around the list of all pages. This represents pages.json
 * @property count total number of pages
 * @property lastUpdated last updated date/time for the pages.json file
 * @property pages list of meta-data objects for each page
 */
/*@Serializable
data class PageList(val count: Int = 0, val lastUpdated: Instant, val pages: List<PageMeta>)*/

/**
 * Wrapper around the list of all templates. This represents templates.json
 * @property count total number of templates
 * @property lastUpdated last updated date/time for the templates.json file
 * @property templates list of all meta-data objects for each template
 */
@Serializable
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
 * @property key the full S3 key for this template ('sources/templates/myTemplate.hbs'). Note that [PageMeta] and [PostMeta] only refer to the leaf of this, i.e. 'myTemplate'
 * @property name user-friendly name of the template
 * @property sections list of strings representing different sections of the page template
 * @property lastUpdated internal property updated whenever the template is saved.
 */
@Serializable
data class Template(val key: String, val name: String, val lastUpdated: Instant = Clock.System.now(), val sections: List<String> = emptyList())

/**
 * Represents the frontmatter metadata for a handlebars template file
 * @property name user-friendly name of the template
 * @property sections list of custom section names for the template
 */
@Serializable
data class TemplateMetadata(val name: String, val sections: List<String> = emptyList())

/**
 * Wrapper class for [Template] which also contains the full body of the Handlebars content
 * @property template the [Template]
 * @property body the full contents of the handlebars file.
 */
@Serializable
data class HandlebarsContent(val template: Template, val body: String)

@Deprecated(message = "This is obsolete, not used.")
@Serializable
data class Layouts(val templates: MutableMap<String, Template>)

@Deprecated(message = "Not implemented yet, not used.")
@Serializable
data class Project(val name: String)
