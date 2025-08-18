package org.liamjd.cantilever.api

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test
import org.koin.test.KoinTest

class LoggingTest : KoinTest {

    @Test
    fun `test logger initialization and usage`() {
        // Given
        val logger = LogManager.getLogger(LoggingTest::class.java)
        
        // When - Log some messages
        logger.info("Test info message")
        logger.warn("Test warning message")
        logger.error("Test error message")
        
        // Then - No exceptions should be thrown
        // This test simply verifies that the logging configuration is valid
        // and doesn't throw exceptions when used
    }
}