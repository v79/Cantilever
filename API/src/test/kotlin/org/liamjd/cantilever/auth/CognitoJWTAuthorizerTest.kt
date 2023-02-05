package org.liamjd.cantilever.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CognitoJWTAuthorizerTest {

    @Test
    fun `should not authorize as token has expired`() {
        val authorizer = CognitoJWTAuthorizer
        val authHeader = "Authorization"
        val bearer = "Bearer "
        val token = """eyJraWQiOiJlWERpXC81aVZiMTFmdWc1cmlYYTNHRndXSzBjZWlBYjNOMTN6Y0NxVFlkaz0iLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiUE9CQTlFTXNVbHloUGlzQ2JyZUF1ZyIsInN1YiI6ImEwYWQ4NzMxLTQ5OTgtNDAwMS1hZGYwLTFhYTlhZTY2NzQ3NyIsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC5ldS13ZXN0LTIuYW1hem9uYXdzLmNvbVwvZXUtd2VzdC0yX2FTZEZEdlUwaiIsImNvZ25pdG86dXNlcm5hbWUiOiJhMGFkODczMS00OTk4LTQwMDEtYWRmMC0xYWE5YWU2Njc0NzciLCJhdWQiOiI2aWpiNmJnM2hrMjJzZWxxNnJqMmJiNXJtcSIsInRva2VuX3VzZSI6ImlkIiwiYXV0aF90aW1lIjoxNjc1NTg3MjE2LCJuYW1lIjoibGlhbSIsImV4cCI6MTY3NTU5MDgxNiwiaWF0IjoxNjc1NTg3MjE3LCJqdGkiOiI1ZTIwMTI4NC02NzE1LTQ2MTEtODZlNS05MGZlMTE1ZGJhZjYiLCJlbWFpbCI6ImxpYW1qZGF2aXNvbkBnbWFpbC5jb20ifQ.zpbPzGMVBbAjhTcuQktJO1tHq26dscFetiJQFNWiyP_wfza_2kSeniU2a8HqzVBDJRnfHeEjNzontpTuBTCx_0-ExbBVKemW5mPVzlst6EdYxzwjGUNFGbOH_jLD2p9Qd48FCuoJQ2bF3VkCTB54eSXPoQaU0eRelJEmu8t5EBOwLUcXoyAEGYXA2xruq7YZkdyttQypt-agGv4PK0RqEt303IEnbOkeJrWg-BqGgxnpKpDYuNplLoJx_K6e1DitT3nnFIhq0eV3eQ74pddIbOYKWpfANDj4RwGS5kxRaLcytzNFyP3-MBEy-c41NUcVDVBqembt_QEs5RYqr5GD4g"""
        val event =
            APIGatewayProxyRequestEvent().withPath("/auth/hello").withHttpMethod("GET")
                .withHeaders(mapOf(authHeader to (bearer + token)))

        val valid = authorizer.authorize(event)

        assertFalse(valid.authorized)
    }
}