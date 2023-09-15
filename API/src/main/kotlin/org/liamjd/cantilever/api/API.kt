package org.liamjd.cantilever.api

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.liamjd.cantilever.api.controllers.*
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
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
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
    private val postController = PostController(sourceBucket = sourceBucket)
    private val pageController = PageController(sourceBucket = sourceBucket)
    private val templateController = TemplateController(sourceBucket = sourceBucket)
    private val generatorController =
        GeneratorController(sourceBucket = sourceBucket)
    private val projectController = ProjectController(sourceBucket = sourceBucket)

    private val cognitoJWTAuthorizer = CognitoJWTAuthorizer(
        mapOf(
            "cognito_region" to System.getenv("cognito_region"),
            "cognito_user_pools_id" to System.getenv("cognito_user_pools_id")
        )
    )

    companion object {
        const val SRCKEY = "{srcKey}"
    }

    override val router = lambdaRouter {

//        filter = loggingFilter()

        /**
        /warm is an attempt to pre-warm this lambda. /ping is an API Gateway reserved route
         */
        get("/warm") { _: Request<Unit> -> println("Ping received; warming"); ResponseEntity.ok("warming") }.supplies(
            setOf(MimeType.plainText)
        )

        auth(cognitoJWTAuthorizer) {
            group("/project") {
                get("/", projectController::getProject)
                put("/", projectController::updateProjectDefinition).expects(setOf(MimeType.yaml)).supplies(setOf(
                    MimeType.yaml))
                group("/posts") {
                    get("", projectController::getPosts)
                    put(
                        "/rebuild", projectController::rebuildPostList
                    )
                }
                group("/pages") {
                    get("", projectController::getPages)
                    post("/", pageController::saveMarkdownPageSource).supplies(setOf(MimeType.plainText))
                    put("/rebuild", projectController::rebuildPageList)
                    get("/$SRCKEY", pageController::loadMarkdownSource)
                }
                get("/templates", projectController::getTemplates)
                put("/templates/rebuild", projectController::rebuildTemplateList)
            }
        }

        auth(cognitoJWTAuthorizer) {
            group("/posts") {
                get("/$SRCKEY", postController::loadMarkdownSource)
                get("/preview/$SRCKEY") { request: Request<Unit> -> ResponseEntity.notImplemented(body = "Not actually returning a preview of ${request.pathParameters["srcKey"]} yet!") }.supplies(
                    setOf(
                        MimeType.html
                    )
                )
                post("/", postController::saveMarkdownPost).supplies(
                    setOf(
                        MimeType.plainText
                    )
                )
                delete("/$SRCKEY", postController::deleteMarkdownPost).supplies(setOf(MimeType.plainText))
            }
        }

        auth(cognitoJWTAuthorizer) {
            group("/templates") {
                get("/$SRCKEY", templateController::loadHandlebarsSource)
                post("/", templateController::saveTemplate).supplies(setOf(MimeType.plainText))
            }
        }

        auth(cognitoJWTAuthorizer) {
            group("/generate") {
                put("/post/$SRCKEY", generatorController::generatePost).supplies(setOf(MimeType.plainText))
                put("/page/$SRCKEY", generatorController::generatePage).supplies(setOf(MimeType.plainText))
                put(
                    "/template/{templateKey}", generatorController::generateTemplate
                ).supplies(setOf(MimeType.plainText))
            }
            group("/cache") {
                delete("/posts") { _: Request<Unit> -> ResponseEntity.notImplemented(body = "Call to delete cache for posts") }
                delete("/pages") { _: Request<Unit> -> ResponseEntity.notImplemented(body = "Call to delete cache for pages") }
            }
        }

        auth(cognitoJWTAuthorizer) {
            group("/get") {
                get("/post/$SRCKEY") { request: Request<Unit> ->
                    ResponseEntity.notImplemented(body = "Received request to return the HTML form of ${request.pathParameters["srcKey"]}")
                }
            }
        }

        get("/showAllRoutes") { _: Request<Unit> ->
            val routeList = this.listRoutes()
            ResponseEntity.ok(routeList)
        }
    }
}

/**
 * Possible extension: custom Filters, like a logging filter, which intercepts a route, performs an action, then passes it on to the correct handler.
 * Something like:
 * `private fun loggingFilter() = Filter { next ->
 *          { request ->
 *              println("Handling request ${request.httpMethod} ${request.path}")
 *              next(request)
 *          }
 *      }`
 */