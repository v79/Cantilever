package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable

/**
 * @property template the handlebars template key (<template>.hbs.html)
 * @property attributes map of custom attributes for the model
 * @property sectionKeys map of custom sections, key=name and value= S3 object key for the fragment
 */
@Serializable
data class PageHandlebarsModelMsg(
    val key: String,
    val template: String,
    val attributes: Map<String, String>,
    val sectionKeys: Map<String, String>
)