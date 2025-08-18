package org.liamjd.cantilever.common

class SystemEnvironmentProvider : EnvironmentProvider {
    override fun getEnv(key: String): String = System.getenv(key) ?: ""
}