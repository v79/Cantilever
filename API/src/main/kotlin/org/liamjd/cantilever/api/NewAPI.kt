package org.liamjd.cantilever.api

import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.liamjd.apiviaduct.routing.*
import org.liamjd.apiviaduct.routing.LambdaRouter
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region

/**
 * Set up koin dependency injection
 */
val cantileverModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
}

class NewLambdaRouter : LambdaRouter() {
    override val corsDomain: String = System.getenv("cors_domain") ?: "https://www.cantilevers.org/"

    init {
        startKoin {
            modules(cantileverModule)
        }
    }


    override val router: Router = lambdaRouter {
        get("/warm") { _: Request<Unit> ->
            println("NEW API: Ping received; warming"); Response.ok(
            "Warmed"
        )}.supplies(MimeType.plainText)
    }
}