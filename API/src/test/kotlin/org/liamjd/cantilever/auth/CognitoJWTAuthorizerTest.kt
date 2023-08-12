package org.liamjd.cantilever.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CognitoJWTAuthorizerTest {

    private val validConfig = mapOf("cognito_region" to "region", "cognito_user_pools_id" to "123")

    @Test
    fun `should not authorize with invalid token`() {
        val authorizer = CognitoJWTAuthorizer(validConfig)
        val authHeader = "Authorization"
        val bearer = "Bearer "
        val token = """a.b.c"""
        val event =
            APIGatewayProxyRequestEvent().withPath("/auth/hello").withHttpMethod("GET")
                .withHeaders(mapOf(authHeader to (bearer + token)))

        val valid = authorizer.authorize(event)

        assertFalse(valid.authorized)
    }

    @Test
    fun `should not authorize with invalid cognito config`() {
        val authorizer = CognitoJWTAuthorizer(emptyMap())
        val authHeader = "Authorization"
        val bearer = "Bearer "
        val token = """a.b.c"""
        val event =
            APIGatewayProxyRequestEvent().withPath("/auth/hello").withHttpMethod("GET")
                .withHeaders(mapOf(authHeader to (bearer + token)))

        val valid = authorizer.authorize(event)

        assertFalse(valid.authorized)
    }
}