package org.liamjd.cantilever.models

import kotlinx.serialization.Serializable

@Serializable
class MarkdownPost(val post: Post) {
    var body: String = ""
}