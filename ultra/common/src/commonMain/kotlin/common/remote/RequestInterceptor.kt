package de.peekandpoke.ultra.common.remote

interface RequestInterceptor {
    fun intercept(request: RemoteRequest)
}
