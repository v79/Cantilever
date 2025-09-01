package org.liamjd.cantilever.lambda.handlebars

import com.github.jknack.handlebars.Parser
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.cache.TemplateCache
import com.github.jknack.handlebars.io.TemplateSource
import org.liamjd.cantilever.common.stripFrontMatter
import java.nio.charset.Charset

/**
 * A cache implementation that strips the YAML front matter from the template
 * As we are running in a short-lived lambda function, we don't actually cache anything.
 */
class YamlStrippingCache : TemplateCache {
    override fun clear() {
        // does nothing
    }

    override fun evict(p0: TemplateSource?) {
        // does nothing
    }

    override fun get(
        source: TemplateSource,
        parser: Parser
    ): Template {
        val strippedSource = StrippedTemplateSource(source)
        return parser.parse(strippedSource)
    }

    override fun setReload(reload: Boolean): TemplateCache {
        return this
    }
}

/**
 * A [TemplateSource] implementation that strips the YAML front matter from the template
 */
class StrippedTemplateSource(val original: TemplateSource) : TemplateSource {
    override fun content(p0: Charset?): String {
        return original.content(p0).stripFrontMatter()
    }

    override fun filename(): String {
        return original.filename()
    }

    override fun lastModified(): Long {
        return original.lastModified()
    }
}