package de.peekandpoke.common.remote

interface ResponseInterceptor {
    suspend fun intercept(response: RemoteResponse): RemoteResponse
}
