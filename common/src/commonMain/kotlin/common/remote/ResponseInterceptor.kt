package de.peekandpoke.ultra.common.remote

interface ResponseInterceptor {
    suspend fun intercept(response: RemoteResponse): RemoteResponse
}
