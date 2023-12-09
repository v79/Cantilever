package org.liamjd.cantilever.openapi

import org.junit.jupiter.api.Test

class OPenAPISchemaProcessorTest {

    @Test
    fun `can find classes with APISchema annotation`() {
        val provider = OpenAPISchemaProcessorProvider()
//        val processor = OpenAPISchemaProcessor(provider.create(null))


    }
}

@APISchema
data class TestClass(val name: String, val age: Int)