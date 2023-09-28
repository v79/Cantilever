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
@Serializable
data class PageList(val count: Int = 0, val folders: List<String> = emptyList(), val lastUpdated: Instant, val pages: List<PageMeta>) {
    fun grouped(): Map<String, List<PageMeta>> {
        return pages.groupBy(keySelector = {it.srcKey})
    }
}

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
 * PageMeta is a summary class representing an individual Page. It does not contain the content for each section; they are flattened to empty strings
 * @property title user-provided title
 * @property srcKey the full S3 key for the page, must be unique, in format 'sources/pages/folder/leafname.md'
 * @property templateKey the leaf of the S3 key for the template this page is based on (e.g. if the full template key is /sources/templates/myTemplate.hbs then this value will be 'myTemplate'
 * @property url the calculated url for this page, initially based on the title, can be overridden, and needs to reflect the folder structure the source file is stored in in S3
 * @property attributes a map of custom attributes, both key and value
 * @property sections a map of the custom sections in the page, but the value is simply stored as an empty string
 * @property lastUpdated internal property updated whenever the page is saved
 *
 */
@Serializable
data class PageMeta(
    val title: String,
    val srcKey: String,
    val templateKey: String,
    val url: String,            // generated url, in format '/folder/leafname' (no .html unless it's index.html)
    val attributes: Map<String, String>,
    val sections: Map<String, String>,
    val lastUpdated: Instant = Clock.System.now()
)

/**
 * Represents a Handlebars template file. It does not contain the content. There is no yaml front-matter, and hence no metadata, for Templates.
 * @property key the full S3 key for this template ('sources/templates/myTemplate.hbs'). Note that [PageMeta] and [PostMeta] only refer to the leaf of this, i.e. 'myTemplate'
 * @property lastUpdated internal property updated whenever the template is saved.
 */
@Serializable
data class Template(val key: String, val lastUpdated: Instant = Clock.System.now())

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
