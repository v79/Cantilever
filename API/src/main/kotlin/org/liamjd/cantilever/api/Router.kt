package org.liamjd.cantilever.api

import kotlinx.serialization.Serializable
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.liamjd.cantilever.api.controllers.StructureController
import org.liamjd.cantilever.api.services.StructureService
import org.liamjd.cantilever.auth.CognitoJWTAuthorizer
import org.liamjd.cantilever.routing.*
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region

/**
 * Set up koin dependency injection
 */
val appModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single { StructureService() }
}

/**
 * The Router class responds to the AWS API Gateway {proxy+} "path" parameter
 */
class LambdaRouter : RequestHandlerWrapper() {

    val sourceBucket = System.getenv("source_bucket")
    val destinationBucket = System.getenv("destination_bucket")
    override val corsDomain: String = System.getenv("cors_domain") ?: "https://www.cantilevers.org/"

    init {
        println("Router init: source bucket: $sourceBucket")
        startKoin {
            modules(appModule)
        }
    }

    // May need some DI here once I start needing to add services for S3 etc
//    private val postController = PostController()
    private val structureController = StructureController(sourceBucket = sourceBucket, corsDomain = corsDomain)

    override val router = lambdaRouter {
//        filter = loggingFilter()


        auth(CognitoJWTAuthorizer) {
            get("/structure", structureController::getStructureFile)
            group("/structure") {
                get("/rebuild", structureController::rebuildStructureFile)
                post("/addSource", structureController::addFileToStructure)
            }
        }

        auth(CognitoJWTAuthorizer) {
            group("/posts") {
                get("load/{srcKey}") { request: Request<Unit> -> ResponseEntity.ok(body = "Retrieving file $request")  }
            }
        }
    }

    /* private fun loggingFilter() = Filter { next ->
         { request ->
             println("Handling request ${request.httpMethod} ${request.path}")
             next(request)
         }
     }*/
}

@Serializable
data class MyResponse(val text: String)

@Serializable
data class MyRequest(val message: String)
