package io.peekandpoke.ultra.remote

/**
 * Intercepts outgoing [RemoteRequest]s before they are sent.
 *
 * Implementations can modify headers, add authentication tokens, or perform
 * other pre-flight adjustments.
 */
interface RequestInterceptor {
    /** Called once for each new [RemoteRequest], allowing mutation before it is sent. */
    fun intercept(request: RemoteRequest)
}
