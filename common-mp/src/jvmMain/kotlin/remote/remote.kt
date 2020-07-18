package de.peekandpoke.common.remote

/**
 * factory method to create a [RemoteRequest]
 */
actual fun remote(
    baseUrl: String,
    requestInterceptors: List<RequestInterceptor>,
    responseInterceptors: List<ResponseInterceptor>
): RemoteRequest = TODO("Not implemented for JVM yet")
