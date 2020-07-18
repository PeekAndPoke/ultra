package de.peekandpoke.common.remote

interface RequestInterceptor {
    fun intercept(request: RemoteRequest)
}
