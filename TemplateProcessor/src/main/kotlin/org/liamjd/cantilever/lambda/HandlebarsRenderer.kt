package org.liamjd.cantilever.lambda

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import org.liamjd.cantilever.lambda.handlebars.LocalDateFormatter
import org.liamjd.cantilever.lambda.handlebars.S3TemplateLoader
import org.liamjd.cantilever.lambda.handlebars.TakeHelper
import org.liamjd.cantilever.lambda.handlebars.YamlStrippingCache
import org.liamjd.cantilever.models.SrcKey
import org.liamjd.cantilever.services.S3Service


interface TemplateRender {
    /**
     * Render the specified template string with the given object map
     * @param model a map of string keys and any entities
     * @param templateString the full string of the template file
     */
    fun renderInline(model: Map<String, Any?>, templateString: String): String

    /**
     * Render the template file with the given object map, loading the template from S3
     * @param model a map of string keys and any entities
     * @param srcKey the S3 key of the template file
     */
    fun render(model: Map<String, Any?>, srcKey: SrcKey): String
}


/**
 * Configuration for Handlebars, defining the helper functions (such as 'upper' and 'localDate') that may be used in templates.
 */
class HandlebarsRenderer(s3Service: S3Service, srcBucket: String) : TemplateRender {

    private val loader = S3TemplateLoader(s3Service, srcBucket)
    private val cache = YamlStrippingCache()
    private val handlebars = Handlebars().with(cache).prettyPrint(true)

    init {
        handlebars.registerHelper("upper", StringHelpers.upper)
        handlebars.registerHelper("localDate", LocalDateFormatter("dd/MM/yyyy"))
        handlebars.registerHelper("take", TakeHelper())
        handlebars.registerHelper("slugify", StringHelpers.slugify)
    }

    override fun renderInline(model: Map<String, Any?>, templateString: String): String {
        // Using compileInline means I won't get any proper error reporting, sadly
        val hbTemplate = handlebars.compileInline(templateString)
        return hbTemplate.apply(model)
    }

    override fun render(model: Map<String, Any?>, srcKey: SrcKey): String {
        val hbTemplate = handlebars.with(loader).compile(srcKey)
        return hbTemplate.apply(model)
    }

}