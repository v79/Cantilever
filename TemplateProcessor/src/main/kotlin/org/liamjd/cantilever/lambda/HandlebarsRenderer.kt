package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.github.jknack.handlebars.Handlebars

/**
 * Render the specified template with the given object map
 * @param model a map of string keys and any entities
 * @param template the full string of the template file
 */
interface TemplateRender {
    fun render(model: Map<String, Any?>, template: String): String
}

context(LambdaLogger)
class HandlebarsRenderer : TemplateRender {

    private val handlebars = Handlebars()

    override fun render(model: Map<String, Any?>, template: String): String {
        log("HandlebarsRenderer processing model (${model.size} entries) for template ${template.substring(0..(if (template.length < 100) template.length-1 else 100))}")
        val hbTemplate = handlebars.compileInline(template)

        return hbTemplate.apply(model)
    }

}