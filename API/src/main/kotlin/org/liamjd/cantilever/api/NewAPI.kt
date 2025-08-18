package org.liamjd.cantilever.api

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.liamjd.apiviaduct.routing.*
import org.liamjd.cantilever.api.controllers.*
import org.liamjd.cantilever.auth.CognitoJWTAuthorizer
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.DynamoDBServiceImpl
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

/**
 * Set up koin dependency injection
 */
val cantileverModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
    single<DynamoDBService> {
        DynamoDBServiceImpl(
            Region.EU_WEST_2, dynamoDbClient = DynamoDbAsyncClient.create()
        )
    }
}

@Suppress("unused")
class NewLambdaRouter : LambdaRouter() {
    private val logger: Logger = LogManager.getLogger(NewLambdaRouter::class.java)
    override val corsDomain: String = System.getenv("cors_domain") ?: "https://www.cantilevers.org/"
    private val sourceBucket: String = System.getenv("source_bucket")
    private val generationBucket: String = System.getenv("generation_bucket")

    init {
        startKoin {
            modules(cantileverModule)
        }
        logger.info("NewLambdaRouter initialized with source bucket: $sourceBucket, generation bucket: $generationBucket")
    }

    // Set up controllers; better would be to use DI
    private val projectController = ProjectController(sourceBucket = sourceBucket, generationBucket = generationBucket)
    private val postsController = PostController(sourceBucket = sourceBucket, generationBucket = generationBucket)
    private val pageController = PageController(sourceBucket = sourceBucket, generationBucket = generationBucket)
    private val templateController =
        TemplateController(sourceBucket = sourceBucket, generationBucket = generationBucket)
    private val mediaController = MediaController(sourceBucket = sourceBucket, generationBucket = generationBucket)
    private val generatorController =
        GeneratorController(sourceBucket = sourceBucket, generationBucket = generationBucket)
    private val metadataController =
        MetadataController(sourceBucket = sourceBucket, generationBucket = generationBucket)

    // authenticator
    private val cognitoJWTAuthorizer = CognitoJWTAuthorizer(
        mapOf(
            "cognito_region" to System.getenv("cognito_region"),
            "cognito_user_pools_id" to System.getenv("cognito_user_pools_id")
        )
    )

    override val router: Router = lambdaRouter {
        /** ============== /warm ================== **/
        get("/warm") { _: Request<Unit> ->
            logger.info("NEW API: Ping received; warming")
            Response.ok("Warmed")
        }.supplies(MimeType.plainText)

        /** ============== /project ================== **/
        group("/project") {
            auth(cognitoJWTAuthorizer) {
                get("/load/{projectKey}", projectController::getProject)
                get("/list", projectController::getProjectList)
                put("/", projectController::updateProjectDefinition).expects(MimeType.yaml)
                post("/new", projectController::createProject).supplies(MimeType.json).expects(MimeType.yaml)
            }
        }

        /** ============== /posts ================== **/
        group("/posts") {
            auth(cognitoJWTAuthorizer) {
                get("", postsController::getPosts)
                get("/{srcKey}", postsController::loadMarkdownSource)
                post("/save", postsController::saveMarkdownPost).supplies(MimeType.plainText)
                delete("/{srcKey}", postsController::deleteMarkdownPost)
                get("/preview/{srcKey}") { _: Request<Unit> ->
                    Response.notImplemented("Preview not implemented")
                }
            }
        }

        /** ============== /pages ================== **/
        group("/pages") {
            auth(cognitoJWTAuthorizer) {
                get("", pageController::getPages)
                get("/{srcKey}", pageController::loadMarkdownSource)
                post("/save", pageController::saveMarkdownPageSource).supplies(MimeType.plainText)
                put("/folder/new/{folderName}", pageController::createFolder).supplies(MimeType.plainText)
                delete("/{srcKey}", pageController::deleteMarkdownPageSource).supplies(MimeType.plainText)
                delete("/folder/{srcKey}", pageController::deleteFolder).supplies(MimeType.plainText)
                post("/reassignIndex", pageController::reassignIndex).supplies(MimeType.plainText)
            }
        }

        /** ============== /folders ================== **/
        auth(cognitoJWTAuthorizer) {
            get("/folders", pageController::getFolders)
        }

        /** ============== /template ================== **/
        group("/templates") {
            auth(cognitoJWTAuthorizer) {
                get("", templateController::getTemplates)
                get("/{srcKey}", templateController::loadHandlebarsSource)
                post("/save", templateController::saveTemplate).supplies(MimeType.plainText)
                get("/usage/{srcKey}", templateController::getTemplateUsage)
                delete("/{srcKey}", templateController::deleteTemplate).supplies(MimeType.plainText)
            }
        }

        /** ============== /media ================== **/
        group("/media") {
            auth(cognitoJWTAuthorizer) {
                get("/images", mediaController::getImageList)
                get("/images/{srcKey}/{resolution}", mediaController::getImage)
                post("/images/", mediaController::uploadImage)
                delete("/delete/{srcKey}", mediaController::deleteImage)
            }
        }

        /** ============== /generate ================== **/
        group("/generate") {
            auth(cognitoJWTAuthorizer) {
                put("/post/{srcKey}", generatorController::generatePost).supplies(MimeType.plainText)
                put("/page/{srcKey}", generatorController::generatePage).supplies(MimeType.plainText)
                put("/template/{templateKey}", generatorController::generateTemplate).supplies(MimeType.plainText)
                delete("/fragments", generatorController::clearGeneratedFragments).supplies(MimeType.plainText)
                delete("/images", generatorController::clearGeneratedImages).supplies(MimeType.plainText)
            }
        }

        /** ============== /metadata ================== **/
        group("/metadata") {
            auth(cognitoJWTAuthorizer) {
                put("/rebuild", metadataController::rebuildFromSources).expects(emptySet())
            }
        }
    }
}