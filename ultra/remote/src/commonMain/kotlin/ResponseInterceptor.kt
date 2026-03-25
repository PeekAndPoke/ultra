package io.peekandpoke.ultra.remote

interface ResponseInterceptor {
    suspend fun intercept(response: RemoteResponse): RemoteResponse
}
