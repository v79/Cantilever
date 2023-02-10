package org.liamjd.cantilever.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CognitoJWTAuthorizerTest {

    @Test
    fun `should not authorize with invalid token`() {
        val authorizer = CognitoJWTAuthorizer
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