package de.peekandpoke.ultra.common.remote

import io.ktor.client.*

expect fun createRequest(
    baseUrl: String,
    requestInterceptors: List<RequestInterceptor>,
    responseInterceptors: List<ResponseInterceptor>,
    client: HttpClient?,
): RemoteRequest
