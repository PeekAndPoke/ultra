package io.peekandpoke.ultra.remote

import io.ktor.client.*

/**
 * Platform-specific URI component encoding.
 *
 * On JS this delegates to the global `encodeURIComponent` function;
 * on JVM it uses [java.net.URLEncoder].
 */
expect fun encodeURIComponent(value: String): String

/**
 * Platform-specific factory for creating a [RemoteRequest].
 *
 * @param baseUrl            the base URL prepended to every request URI
 * @param requestInterceptors interceptors applied before each request is sent
 * @param responseInterceptors interceptors applied after each response is received
 * @param client             optional Ktor [HttpClient] used by the platform implementation
 */
expect fun createRequest(
    baseUrl: String,
    requestInterceptors: List<RequestInterceptor>,
    responseInterceptors: List<ResponseInterceptor>,
    client: HttpClient?,
): RemoteRequest
