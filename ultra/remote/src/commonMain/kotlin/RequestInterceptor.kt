package io.peekandpoke.ultra.remote

interface RequestInterceptor {
    fun intercept(request: RemoteRequest)
}
