package org.liamjd.cantilever.api

import kotlinx.serialization.Serializable
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.liamjd.cantilever.api.controllers.GeneratorController
import org.liamjd.cantilever.api.controllers.PostController
import org.liamjd.cantilever.api.controllers.StructureController
import org.liamjd.cantilever.api.services.StructureService
import org.liamjd.cantilever.auth.CognitoJWTAuthorizer
import org.liamjd.cantilever.routing.*
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region

/**
 * Set up koin dependency injection
 */
val appModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2)}
    single { StructureService() }
}

/**
 * The Router class responds to the AWS API Gateway {proxy+} "path" parameter
 */
class LambdaRouter : RequestHandlerWrapper() {

    private val sourceBucket: String = System.getenv("source_bucket")
    private val destinationBucket: String = System.getenv("destination_bucket")
    override val corsDomain: String = System.getenv("cors_domain") ?: "https://www.cantilevers.org/"

    init {
        startKoin {
            modules(appModule)
        }
    }

    // May need some DI here once I start needing to add services for S3 etc
    private val structureController = StructureController(sourceBucket = sourceBucket, corsDomain = corsDomain)
    private val postController = PostController(sourceBucket = sourceBucket, destinationBucket = destinationBucket)
    private val generatorController = GeneratorController(sourceBucket = sourceBucket, destinationBucket = destinationBucket)

    override val router = lambdaRouter {
//        filter = loggingFilter()

        // /warm is an attempt to pre-warm this lambda. /ping is an API Gateway reserved route
        get("/warm") { _: Request<Unit> -> println("Ping received; warming"); ResponseEntity.ok("warming") }.supplies(
            setOf(MimeType.plainText)
        )

        auth(CognitoJWTAuthorizer) {
            get("/structure", structureController::getStructureFile)
            group("/structure") {
                get("/rebuild", structureController::rebuildStructureFile)
                post("/addSource", structureController::addFileToStructure)
            }
        }

        auth(CognitoJWTAuthorizer) {
            group("/posts") {
                get("/load/{srcKey}", postController::loadMarkdownSource)
                get("/preview/{srcKey}") { request: Request<Unit> -> ResponseEntity.notImplemented(body = "Not actually returning a preview of ${request.pathParameters["srcKey"]} yet!") }.supplies(
                    setOf(
                        MimeType.html
                    )
                )
                post("/save", postController::saveMarkdownPost).supplies(
                    setOf(
                        MimeType.plainText
                    )
                )
                delete("/{srcKey}", postController::deleteMarkdownPost).supplies(setOf(MimeType.plainText))
            }
        }

        auth(CognitoJWTAuthorizer) {
            group("/generate") {
                put("/post/{srcKey}") { request: Request<Unit> ->
                    ResponseEntity.notImplemented(body = "This route should trigger a regenerate of an existing markdown post ${request.pathParameters["srcKey"]}")
                }
                put("/page/{srcKey}", generatorController::generatePage).supplies(setOf(MimeType.plainText))
                put("/template/{templateKey}") { request: Request<Unit> ->
                    ResponseEntity.notImplemented(body = "This route should trigger a regenerate of all static pages which use the given template ${request.pathParameters["templateKey"]}")
                }
            }
            group("/cache") {
                delete("/posts") { _: Request<Unit> -> ResponseEntity.notImplemented(body = "Call to delete cache for posts")}
                delete("/pages") { _: Request<Unit> -> ResponseEntity.notImplemented(body = "Call to delete cache for pages")}
            }
        }

        auth(CognitoJWTAuthorizer) {
            group("/get") {
                get("/post/{srcKey}") {
                    request: Request<Unit> ->
                    ResponseEntity.notImplemented(body = "Received request to return the HTML form of ${request.pathParameters["srcKey"]}")
                }
            }
        }

        get("/showAllRoutes") { _: Request<Unit> ->
            val routeList = this.listRoutes()
            ResponseEntity.ok(routeList)
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
