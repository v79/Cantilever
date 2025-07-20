package org.liamjd.cantilever.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.RSAKeyProvider
import org.liamjd.apiviaduct.routing.AuthResult
import org.liamjd.apiviaduct.routing.AuthType
import org.liamjd.cantilever.routing.getHeader
import java.net.MalformedURLException
import java.net.URL
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * General interface for any Authorizer class
 * @property simpleName a user-friendly name for the authorizer; not functionally relevant
 * Only one method, `authorize`, which returns an [AuthResult]
 */
@Deprecated("Use org.liamjd.api.routing.Authorizer instead")
interface Authorizer {
    val simpleName: String
    val type: String
    fun authorize(request: APIGatewayProxyRequestEvent): AuthResult

    fun info(message: String) = println("INFO: Authorizer: $message")
    fun warn(message: String) = println("WARN: Authorizer: $message")
    fun logError(message: String) = println("ERROR: Authorizer: $message")
}


/**
 * Looks for the 'Authorizer' header, with the value 'Bearer <token>'
 * and verify that it is legitimate for the Cognito user pool
 */
class CognitoJWTAuthorizer(private val configuration: Map<String, String>) : org.liamjd.apiviaduct.routing.Authorizer {

    override val simpleName: String
        get() = "CognitoJWT Bearer Token Authorizer"
    override val type: AuthType = AuthType.HTTP

    override fun authorize(request: APIGatewayProxyRequestEvent): AuthResult {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null) {
            logError("Missing Authorization Header")
            return AuthResult(false, "Missing Authorization Header")
        }
        val token = extractToken(authHeader)
        if (token == "") return AuthResult(false, "Invalid or missing Bearer token")

        val awsCognitoRegion = configuration["cognito_region"]
        val awsUserPoolsId = configuration["cognito_user_pools_id"]
        if (awsCognitoRegion.isNullOrEmpty() || awsUserPoolsId.isNullOrEmpty()) {
            logError("Could not verify credentials; Cognito region and/or user pool ID not configured: $configuration")
            return AuthResult(
                false,
                "Could not verify credentials; Cognito region and/or user pool ID not configured."
            )
        } else {
            val keyProvider: RSAKeyProvider = AWSCognitoRSAKeyProvider(awsCognitoRegion, awsUserPoolsId)
            val algorithm: Algorithm = Algorithm.RSA256(keyProvider)
            val jwtVerifier: JWTVerifier =
                JWT.require(algorithm) //.withAudience("2qm9sgg2kh21masuas88vjc9se") // Validate your apps audience if needed
                    .build()

            val verified = try {
                jwtVerifier.verify(token)
            } catch (veriException: JWTVerificationException) {
                logError("Verification of token failed; exception type is: ${veriException::class}")
                logError(veriException.message ?: "<No exception message found>")
                return AuthResult(false, veriException.message.toString())
            }
            info("Authorized user ${verified.getClaim("name")}, token with claims: ${verified.claims}")
            return AuthResult(true, "")
        }

    }

    private fun extractToken(header: String): String {
        return header.substringAfter("Bearer ", "")
    }

    private fun info(message: String) = println("INFO: Authorizer: $message")
    private fun logError(message: String) = println("ERROR: Authorizer: $message")

}

internal class AWSCognitoRSAKeyProvider(cognitoRegion: String, userPoolID: String) : RSAKeyProvider {

    private val provider: JwkProvider
    private val awsKidStoreUrl: URL

    init {
        val url =
            String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", cognitoRegion, userPoolID)
        try {
            awsKidStoreUrl = URL(url)
        } catch (mue: MalformedURLException) {
            throw RuntimeException("Invalid URL provided, URL = $url")
        }
        provider = JwkProviderBuilder(awsKidStoreUrl).build()
    }

    override fun getPublicKeyById(kid: String?): RSAPublicKey {
        return try {
            provider.get(kid).publicKey as RSAPublicKey
        } catch (jwke: JwkException) {
            throw RuntimeException(
                String.format(
                    "Failed to get JWT kid=%s from aws_kid_store_url=%s",
                    kid,
                    awsKidStoreUrl
                )
            )
        }
    }

    override fun getPrivateKey(): RSAPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getPrivateKeyId(): String {
        return ""
    }
}