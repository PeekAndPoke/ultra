package io.peekandpoke.ultra.remote

/**
 * Intercepts incoming [RemoteResponse]s after they are received.
 *
 * Implementations can log errors, transform the response, or trigger side-effects.
 */
interface ResponseInterceptor {
    /** Called for each [RemoteResponse], returning a potentially modified response. */
    suspend fun intercept(response: RemoteResponse): RemoteResponse
}
