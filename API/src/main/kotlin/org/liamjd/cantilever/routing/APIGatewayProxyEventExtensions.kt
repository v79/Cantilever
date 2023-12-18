package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.liamjd.cantilever.common.MimeType

/**
 * General helper function to get a named header
 */
fun APIGatewayProxyRequestEvent.getHeader(httpHeader: String): String? =
    this.headers?.entries?.firstOrNull { httpHeader.equals(it.key, ignoreCase = true) }?.value

fun APIGatewayProxyRequestEvent.acceptHeader() = getHeader("accept")
fun APIGatewayProxyRequestEvent.xContentLengthHeader() = getHeader("X-Content-Length")
fun APIGatewayProxyRequestEvent.contentType() = getHeader("content-type")

/**
 * Split the 'accept' header into a list of [MimeType]
 */
fun APIGatewayProxyRequestEvent.acceptedMediaTypes() =
    acceptHeader()?.split(",")?.map { it.trim() }?.mapNotNull { MimeType.parse(it) }.orEmpty()