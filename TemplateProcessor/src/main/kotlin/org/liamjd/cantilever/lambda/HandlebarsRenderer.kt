package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import org.liamjd.cantilever.lambda.handlebars.LocalDateFormatter


interface TemplateRender {
    /**
     * Render the specified template with the given object map
     * @param model a map of string keys and any entities
     * @param template the full string of the template file
     */
    fun render(model: Map<String, Any?>, template: String): String
}

context(LambdaLogger)
class HandlebarsRenderer : TemplateRender {

    private val handlebars = Handlebars()

    init {
        handlebars.registerHelper("upper", StringHelpers.upper)
/*        handlebars.registerHelper("forEach", ForEachHelper())
        handlebars.registerHelper("paginate", Paginate())*/
        handlebars.registerHelper("localDate", LocalDateFormatter("dd/MM/yyyy"))
        handlebars.registerHelper("capitalize",StringHelpers.capitalize)
        handlebars.registerHelper("slugify", StringHelpers.slugify)
    }

    override fun render(model: Map<String, Any?>, template: String): String {
        log("HandlebarsRenderer processing model (${model.size} entries) for template ${template.take(100)}")
        val hbTemplate = handlebars.compileInline(template)

        return hbTemplate.apply(model)
    }

}