package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service

abstract class APIController(val sourceBucket: String, val generationBucket: String) : KoinComponent {

    val s3Service: S3Service by inject()
    val dynamoDBService: DynamoDBService by inject()

    /**
     * A slightly nicer logging mechanism than println?
     * Override these to add the class name?
     */
    open fun info(message: String) = println(message)
    open fun warn(message: String) = println("WARN: $message")
    open fun error(message: String) = println("ERROR: $message")
    open fun debug(message: String) = println("DEBUG: $message")
}