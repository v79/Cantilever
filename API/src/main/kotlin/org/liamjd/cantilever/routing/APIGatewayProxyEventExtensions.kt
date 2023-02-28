package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent

data class Header(val name: String, val value: String)

fun APIGatewayProxyRequestEvent.acceptHeader() = getHeader("accept")
fun APIGatewayProxyRequestEvent.contentLengthHeader() = getHeader("Content-Length")


fun APIGatewayProxyRequestEvent.getHeader(httpHeader: String): String? =
    this.headers?.entries?.firstOrNull { httpHeader.equals(it.key, ignoreCase = true) }?.value
fun APIGatewayProxyRequestEvent.contentType() = getHeader("content-type")

/**
 * Split the 'accept' header into a list of [MimeType]
 */
fun APIGatewayProxyRequestEvent.acceptedMediaTypes() =
    acceptHeader()?.split(",")?.map { it.trim() }?.mapNotNull { MimeType.parse(it) }.orEmpty()