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
import org.liamjd.cantilever.routing.getHeader
import java.net.MalformedURLException
import java.net.URL
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey


interface Authorizer {
    val simpleName: String
    fun authorize(request: APIGatewayProxyRequestEvent): AuthResult
}

data class AuthResult(val authorized: Boolean, val message: String)

/**
 * Looks for the 'Authorizer' header, with the value 'Bearer <token>'
 * and verify that it is legitimate for the Cognito user pool
 */
object CognitoJWTAuthorizer : Authorizer {
    override val simpleName: String
        get() = "CognitoJWT Bearer Token Authorizer"

    override fun authorize(request: APIGatewayProxyRequestEvent): AuthResult {
        val authHeader = request.getHeader("Authorization")
        println("CognitoJWTAuthenticator: Bearer = $authHeader")
        if (authHeader == null) return AuthResult(false,"Missing Authorization Header")
        val token = extractToken(authHeader)
        if (token == "") return AuthResult(false,"Invalid or missing Bearer token")

        // TODO: externalise these
        val awsCognitoRegion = "eu-west-2"
        val awsUserPoolsId = "eu-west-2_aSdFDvU0j"

        val keyProvider: RSAKeyProvider = AWSCognitoRSAKeyProvider(awsCognitoRegion, awsUserPoolsId)
        val algorithm: Algorithm = Algorithm.RSA256(keyProvider)
        val jwtVerifier: JWTVerifier =
            JWT.require(algorithm) //.withAudience("2qm9sgg2kh21masuas88vjc9se") // Validate your apps audience if needed
                .build()

        val verified = try {
            jwtVerifier.verify(token)
        } catch (veriException: JWTVerificationException) {
            println("Verification of token failed; exception type is: ${veriException::class}")
            println(veriException.message)
            return AuthResult(false,veriException.message.toString())
        }
        println("Authorized user ${verified.getClaim("name")}, token with claims: ${verified.claims}")
        return AuthResult(true,"")
    }

    private fun extractToken(header: String): String {
        return header.substringAfter("Bearer ", "")
    }


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
            println(jwke.message)
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