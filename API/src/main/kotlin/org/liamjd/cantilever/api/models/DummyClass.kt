package org.liamjd.cantilever.api.models

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.openapi.APISchema

@Serializable
@APISchema
class DummyClass(val name: String, val age: Int) {
    var title: String = "dummy title"
}