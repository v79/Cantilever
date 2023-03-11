package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Structure(val layouts: Layouts, val posts: MutableList<Post>, var postCount: Int)

@Serializable
data class PostList(val count: Int = 0, val posts: List<Post>)

@Serializable
data class Post(val title: String, val srcKey: String, val url: String, val date: LocalDate, val lastUpdated: Instant = Clock.System.now(), val templateKey: String)

@Serializable
data class Template(val key: String, val lastUpdated: Instant = Clock.System.now())

@Serializable
data class Layouts(val templates: MutableMap<String, Template>)

@Serializable
data class Project(val name: String)
