package io.peekandpoke.ultra.remote

class ErrorLoggingResponseInterceptor : ResponseInterceptor {
    override suspend fun intercept(response: RemoteResponse): RemoteResponse {
        if (!response.ok) {
            console.error(response)
        }

        return response
    }
}
