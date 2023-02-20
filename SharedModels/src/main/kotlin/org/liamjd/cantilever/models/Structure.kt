package org.liamjd.cantilever.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.common.now

@Serializable
data class Structure(val layouts: Layouts, val posts: MutableList<Post>, var postCount: Int)

@Serializable
data class Post(val title: String, val srcKey: String, val url: String, val template: Template, val date: LocalDate, val lastUpdated: LocalDateTime = LocalDateTime.now())

@Serializable
data class Template(val key: String, val lastUpdated: LocalDateTime)

@Serializable
data class Layouts(val templates: MutableMap<String, Template>)

@Serializable
data class Project(val name: String)
