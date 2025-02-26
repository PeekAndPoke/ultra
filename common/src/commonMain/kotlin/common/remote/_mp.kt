package de.peekandpoke.ultra.common.remote

import io.ktor.client.*

expect fun encodeURIComponent(value: String): String

expect fun createRequest(
    baseUrl: String,
    requestInterceptors: List<RequestInterceptor>,
    responseInterceptors: List<ResponseInterceptor>,
    client: HttpClient?,
): RemoteRequest
