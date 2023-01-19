package org.liamjd.cantilever.api

import io.moia.router.Filter
import io.moia.router.RequestHandler
import io.moia.router.Router.Companion.router
import org.liamjd.cantilever.api.controllers.PostController

/**
 * The Router class responds to the AWS API Gateway {proxy+} "path" parameter
 */
class Router : RequestHandler() {

    // May need some DI here once I start needing to add services for S3 etc
    private val postController = PostController()

    override val router = router {
        filter = loggingFilter()

        post("/newPost", postController::newPost)
    }

    private fun loggingFilter() = Filter { next ->
        { request ->
            println("Handling request ${request.httpMethod} ${request.path}")
            next(request)
        }
    }
}

data class MyResponse(val text: String)
data class MyRequest(val message: String)
