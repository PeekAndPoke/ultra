package io.peekandpoke.ultra.remote

/**
 * A [ResponseInterceptor] that logs non-OK responses to the browser console as errors.
 *
 * This is a JS-only interceptor intended for development / debugging purposes.
 */
class ErrorLoggingResponseInterceptor : ResponseInterceptor {
    override suspend fun intercept(response: RemoteResponse): RemoteResponse {
        if (!response.ok) {
            console.error(response)
        }

        return response
    }
}
