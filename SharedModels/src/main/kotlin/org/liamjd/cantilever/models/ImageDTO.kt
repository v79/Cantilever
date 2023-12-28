package org.liamjd.cantilever.models

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.openapi.APISchema

/**
 * A DTO for an image, containing the S3 key and the base64 encoded image
 */
@APISchema
@Serializable
class ImageDTO(val srcKey: String, val contentType: String, val bytes: String)