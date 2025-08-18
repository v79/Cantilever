package org.liamjd.cantilever.common

/**
 * Wrapper for System.getEnv and related functions, so that I can mock
 */
interface EnvironmentProvider {
    fun getEnv(key: String): String
}