package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Deprecated(message = "Use separate Post, Page, Template list classes")
@Serializable
data class Structure(val layouts: Layouts, val posts: MutableList<PostMeta>, var postCount: Int)

@Serializable
data class PostList(val count: Int = 0, val lastUpdated: Instant, val posts: List<PostMeta>)

@Serializable
data class PageList(val count: Int = 0, val lastUpdated: Instant, val pages: List<Page>)

@Serializable
data class TemplateList(val count: Int = 0, val lastUpdated: Instant, val templates: List<Template>)

@Serializable
data class PostMeta(
    val title: String,
    val srcKey: String,
    val url: String,
    val date: LocalDate,
    val lastUpdated: Instant = Clock.System.now(),
    val templateKey: String
)

@Serializable
data class Page(
    val title: String,
    val srcKey: String,
    val templateKey: String,
    val url: String,
    val attributes: Map<String,String>,
    val sections: Map<String,String>,
    val lastUpdated: Instant = Clock.System.now()
)

@Serializable
data class Template(val key: String, val lastUpdated: Instant = Clock.System.now())

@Serializable
data class Layouts(val templates: MutableMap<String, Template>)

@Serializable
data class Project(val name: String)
