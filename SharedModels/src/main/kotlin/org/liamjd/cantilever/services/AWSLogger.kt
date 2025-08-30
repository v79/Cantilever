package org.liamjd.cantilever.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.logging.LogLevel

/**
 * Base class for AWS logging. This class provides a simple interface for logging messages and exceptions
 * in AWS Lambda functions. It uses the LambdaLogger if available, otherwise it falls back to standard output.
 * The logging level can be specified, and the source of the message can be included for better context.
 */
abstract class AWSLogger(val enableLogging: Boolean = true, val msgSource: String) {

    abstract var logger: LambdaLogger?

    /**
     * Log a message with the default INFO level
     * @param message The message to log
     */
    fun log(message: String) = log("INFO", message)

    /**
     * Log a message with the specified level
     * @param level The log level (INFO, WARN, ERROR)
     * @param message The message to log
     */
    fun log(level: String = "INFO", message: String) {
        if (enableLogging) {
            logger?.log("[${msgSource}]: $message", LogLevel.fromString(level))
                ?: println("[$level] [${msgSource}]: $message")
        }
    }

    /**
     * Log an exception with the specified level. This will use the LambdaLogger if available, otherwise it will print to standard output.
     * @param level The log level (INFO, WARN, ERROR)
     * @param message The message to log
     * @param e The exception to log
     */
    fun log(level: String, message: String, e: Throwable) {
        if (enableLogging) {
            logger?.apply {
                val lvl = LogLevel.fromString(level)
                this.log("[${msgSource}]: $message", lvl)
                this.log("Exception: ${e.javaClass.simpleName}: ${e.message}", lvl)
                e.stackTrace.take(5).forEach { this.log("[$level]   at $it", lvl) }
            } ?: run {
                println("[$level] [${msgSource}]: $message")
                println("[$level] Exception: ${e.javaClass.simpleName}: ${e.message}")
                e.stackTrace.take(5).forEach { println("[$level]   at $it") }
            }
        }
    }

}