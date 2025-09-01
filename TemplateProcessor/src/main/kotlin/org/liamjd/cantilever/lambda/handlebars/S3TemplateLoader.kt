package org.liamjd.cantilever.lambda.handlebars

import com.github.jknack.handlebars.io.StringTemplateSource
import com.github.jknack.handlebars.io.TemplateLoader
import com.github.jknack.handlebars.io.TemplateSource
import org.liamjd.cantilever.services.S3Service
import java.nio.charset.Charset

/**
 * A custom template loader that loads templates from S3.
 * Set the prefix to the be project domain.
 * @param s3Service the S3 service to use for loading templates
 * @param bucket the S3 bucket to load templates from
 * @see TemplateLoader
 * When resolving template file names, if the name starts with the prefix (e.g. the domain), then it is assumed to be a full S3 key.
 * Otherwise, the name is assumed to be relative and is prefixed with "<prefix>/sources/templates/" and sufficed with the suffix.
 */
class S3TemplateLoader(val s3Service: S3Service, val bucket: String) : TemplateLoader {
    private var prefix = ""
    private var suffix = ""

    override fun sourceAt(srcKey: String): TemplateSource {
        val resolved = resolve(srcKey)
        println("S3TemplateLoader: Loading template '$srcKey' resolved to '${resolved}' from S3")
        if (!s3Service.objectExists(resolved, bucket)) {
            throw IllegalArgumentException("Template '$srcKey' resolved to '$resolved' not found in bucket '$bucket'")
        }
        val templateString = s3Service.getObjectAsString(resolved, bucket)

        return StringTemplateSource(resolved, templateString)
    }

    /**
     * Resolves the template key to an S3 key. If the key starts with the prefix, then it is assumed to be a full S3 key.
     * Otherwise, the key is assumed to be relative and is prefixed with "<prefix>/sources/templates/" and sufficed with the suffix.
     * @param templateKey the template key to resolve
     * @return the resolved S3 key
     */
    override fun resolve(templateKey: String): String {
        val resolved = if (templateKey.startsWith(prefix)) {
            templateKey
        } else {
            "$prefix/sources/templates/$templateKey$suffix"
        }
        println("S3TemplateLoader: Resolving template '$templateKey' to '$resolved'")
        return resolved
    }

    override fun getPrefix(): String = prefix

    override fun getSuffix(): String = suffix

    override fun setPrefix(newPrefix: String) {
        this.prefix = newPrefix
    }

    override fun setSuffix(newSuffix: String) {
        this.suffix = newSuffix
    }

    override fun setCharset(p0: Charset) {
        TODO("Not yet implemented")
    }

    override fun getCharset(): Charset {
        TODO("Not yet implemented")
    }
}