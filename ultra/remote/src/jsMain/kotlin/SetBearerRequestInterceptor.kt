package io.peekandpoke.ultra.remote

/**
 * A [RequestInterceptor] that attaches a Bearer token to every outgoing request.
 *
 * The [token] lambda is evaluated lazily on each request. When it returns `null`,
 * the `Authorization` header is **not** added.
 */
class SetBearerRequestInterceptor(private val token: () -> String?) : RequestInterceptor {

    override fun intercept(request: RemoteRequest) {
        token()?.let {
            request.header("Authorization", "Bearer $it")
        }
    }
}
