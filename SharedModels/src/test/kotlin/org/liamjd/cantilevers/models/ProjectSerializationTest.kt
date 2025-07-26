package org.liamjd.cantilevers.models

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ImgRes

internal class ProjectSerializationTest {

    @Test
    fun `can serialize basic model to yaml`() {
        val proj = CantileverProject(
            domain =  "https://example.com",
            projectName = "Test",
            author = "Me",
            dateTimeFormat = "dd/MM/yyyy HH:mm",
            imageResolutions = mapOf("Normal" to ImgRes(640, 480))
        )

        val yaml = Yaml.default.encodeToString(CantileverProject.serializer(), proj)

        val expected = """
            domain: "https://example.com"
            projectName: "Test"
            author: "Me"
            dateFormat: "dd/MM/yyyy"
            dateTimeFormat: "dd/MM/yyyy HH:mm"
            imageResolutions:
              "Normal": "640x480"
        """.trimIndent()

        assertEquals(expected.trim(), yaml.trim())
    }

    @Test
    fun `can deserialize basic yaml to model`() {
        val yaml = """
            projectName: "Test"
            author: "Me"
            dateFormat: "dd/MM/yyyy"
            dateTimeFormat: "dd/MM/yyyy HH:mm"
            imageResolutions:
              "Normal": "640x480"
            domain: "https://example.com"
        """.trimIndent()

        val project = Yaml.default.decodeFromString<CantileverProject>(yaml)

        assertEquals("Test", project.projectName)
        assertEquals("Me", project.author)
        assertEquals(ImgRes(640, 480), project.imageResolutions["Normal"])
    }

    @Test
    fun `can serialize basic model with custom attributes to yaml`() {
        val proj = CantileverProject(
            domain =  "https://example.com",
            projectName = "Test",
            author = "Me",
            dateTimeFormat = "dd/MM/yyyy HH:mm",
            imageResolutions = mapOf("Normal" to ImgRes(640, 480)),
            attributes = mapOf("Wibble" to "Greep")
        )

        val yaml = Yaml.default.encodeToString(CantileverProject.serializer(), proj)

        val expected = """
            domain: "https://example.com"
            projectName: "Test"
            author: "Me"
            dateFormat: "dd/MM/yyyy"
            dateTimeFormat: "dd/MM/yyyy HH:mm"
            imageResolutions:
              "Normal": "640x480"
            attributes:
              "Wibble": "Greep"
        """.trimIndent()

        assertEquals(expected.trim(), yaml.trim())
    }

    @Test
    fun `can deserialize custom attributes to project model`() {
        val yaml = """
            projectName: "Test"
            author: "Me"
            dateFormat: "dd/MM/yyyy"
            dateTimeFormat: "dd/MM/yyyy HH:mm"
            imageResolutions:
              "Normal": "640x480"
            attributes:
              "Wibble": "Greep"
            domain: "https://example.com"
        """.trimIndent()

        val project = Yaml.default.decodeFromString<CantileverProject>(yaml)

        assertEquals("Test", project.projectName)
        assertEquals("Me", project.author)
        assertEquals(ImgRes(640, 480), project.imageResolutions["Normal"])
        assertNotNull(project.attributes?.get("Wibble"))
        assertEquals("Greep", project.attributes!!["Wibble"])
        assertNotNull(project.domain)
    }
}