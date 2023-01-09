package org.liamjd.cantilever.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Post(val title: String, val srcKey: String, val url: String, val template: Template, val lastUpdated: LocalDateTime)

@Serializable
data class Template(val key: String, val lastUpdated: LocalDateTime)

@Serializable
data class Layouts(val templates: List<Template>)

@Serializable
data class Items(val posts: Map<String,Post>)

@Serializable
data class Structure(val layouts: Layouts, val items: Items)

@Serializable
data class Project(val name: String)