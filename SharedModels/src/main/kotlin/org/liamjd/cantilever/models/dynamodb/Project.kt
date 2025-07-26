package org.liamjd.cantilever.models.dynamodb

import kotlinx.serialization.Transient

class Project(
    val domain: String,
    val projectName: String,
    val author: String,
    val dateFormat: String,
    val dateTimeFormat: String
) {
    @Transient
    val srcKey = "$domain.yaml"
}
