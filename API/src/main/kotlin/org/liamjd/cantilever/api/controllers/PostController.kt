package org.liamjd.cantilever.api.controllers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.models.MarkdownPost
import org.liamjd.cantilever.models.Post
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import java.net.URLDecoder
import java.nio.charset.Charset

class PostController(val sourceBucket: String) : KoinComponent {
    private val s3Service: S3Service by inject()

    fun loadMarkdownSource(request: Request<Unit>): ResponseEntity<APIResult<MarkdownPost>> {
        val markdownSource = request.pathParameters["srcKey"]
        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            println("PostsController loading Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                println("Faking return of complete Markdown post")
                val fakeMDPost = MarkdownPost(
                    Post(
                        title = "Fake title",
                        srcKey = markdownSource,
                        url = "fakeUrl",
                        template = Template(key = "fakeTemplateKey", lastUpdated = LocalDateTime.now()),
                        date = LocalDate.now(),
                        lastUpdated = LocalDateTime.now()
                    )
                )
                ResponseEntity.ok(body = APIResult.Success(fakeMDPost))
            } else {
                println("PostController: File '$decoded' not found")
                ResponseEntity.notFound(body = APIResult.Error("Markdown file $decoded not found in bucket $sourceBucket"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }
}
