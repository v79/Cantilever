package org.liamjd.cantilever.api.controllers

interface APIController {

    /**
     * A slightly nicer logging mechanism than println?
     */
    fun info(message: String) = println(message)
    fun warn(message: String) = println("WARN: $message")
    fun error(message: String) = println("ERROR: $message")
    fun debug(message: String) = println("DEBUG: $message")
}