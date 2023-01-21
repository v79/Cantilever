package org.liamjd.cantilever.api

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.liamjd.cantilever.api.controllers.PostController
import org.liamjd.cantilever.api.controllers.StructureController
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.RequestHandlerWrapper
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.routing.Router
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region

val appModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
}

/**
 * The Router class responds to the AWS API Gateway {proxy+} "path" parameter
 */
class LambdaRouter : RequestHandlerWrapper() {

    val sourceBucket = System.getenv("source_bucket")
    val destinationBucket = System.getenv("destination_bucket")

    init {
        println("Router init: source bucket: $sourceBucket")
        startKoin {
            modules(appModule)
        }
    }

    // May need some DI here once I start needing to add services for S3 etc
    private val postController = PostController()
    private val structureController = StructureController(sourceBucket = sourceBucket)

    override val router = Router.router {
//        filter = loggingFilter()

        get("/route") { r: Request<String> ->
            ResponseEntity.ok(MyResponse(r.body))
        }.expects(null)

        get("/hello") { _: Request<String> ->
            ResponseEntity.ok(body = "Hello")
        }
//        get("/structure", structureController::getStructureFile)

//        post("/newPost", postController::newPost)
    }

    /* private fun loggingFilter() = Filter { next ->
         { request ->
             println("Handling request ${request.httpMethod} ${request.path}")
             next(request)
         }
     }*/
}

data class MyResponse(val text: String)
data class MyRequest(val message: String)
