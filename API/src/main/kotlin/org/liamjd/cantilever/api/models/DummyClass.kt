package org.liamjd.cantilever.api.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.openapi.APISchema

@Serializable
@APISchema
class DummyClass(
    val name: String,
    val age: Int,
    val truth: Boolean,
    val floaty: Float,
    val longy: Long,
    val date: Instant,
    val stringList: List<String>,
    val numArray: Array<Int>
) {
    var title: String = "dummy title"
}