package org.liamjd.cantilever.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.liamjd.cantilever.routing.getHeader

interface Authorizer {
    val simpleName: String
    fun authorize(request: APIGatewayProxyRequestEvent): Boolean
}

/**
 * Looks for the 'Authorizer' header, with the value 'Bearer <token>'
 */
object CognitoJWTAuthorizer : Authorizer {
    override val simpleName: String
        get() = "CognitoJWT Bearer Token Authorizer"

    override fun authorize(request: APIGatewayProxyRequestEvent): Boolean {
        val authHeader = request.getHeader("Authorization")
        println("CognitoJWTAuthenticator: Bearer = $authHeader")
        if (authHeader == null) return false
        val token = extractToken(authHeader)
        if (token == "") return false


        return true
    }

    private fun extractToken(header: String): String {
        return header.substringAfter("Bearer ", "")
    }
}