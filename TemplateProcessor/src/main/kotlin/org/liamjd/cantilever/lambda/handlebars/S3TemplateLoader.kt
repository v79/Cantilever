package org.liamjd.cantilever.lambda.handlebars

import com.github.jknack.handlebars.io.StringTemplateSource
import com.github.jknack.handlebars.io.TemplateLoader
import com.github.jknack.handlebars.io.TemplateSource
import org.liamjd.cantilever.services.S3Service
import java.nio.charset.Charset

/**
 * A custom template loader that loads templates from S3
 * @param s3Service the S3 service to use for loading templates
 * @param bucket the S3 bucket to load templates from
 * @see TemplateLoader
 * Note, I have not fully implemented the TemplateLoader interface yet, as I haven't needed to use it yet.
 */
class S3TemplateLoader(val s3Service: S3Service, val bucket: String) : TemplateLoader
{
    override fun sourceAt(srcKey: String): TemplateSource {
        val templateString = s3Service.getObjectAsString(srcKey, bucket)

        return StringTemplateSource(srcKey, templateString)
    }

    override fun resolve(p0: String?): String {
        TODO("Not yet implemented")
    }

    override fun getPrefix(): String {
        TODO("Not yet implemented")
    }

    override fun getSuffix(): String {
        TODO("Not yet implemented")
    }

    override fun setPrefix(prefix: String) {
        TODO("Not yet implemented")
    }

    override fun setSuffix(p0: String) {
        TODO("Not yet implemented")
    }

    override fun setCharset(p0: Charset) {
        TODO("Not yet implemented")
    }

    override fun getCharset(): Charset {
        TODO("Not yet implemented")
    }
}