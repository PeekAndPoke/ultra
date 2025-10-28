package de.peekandpoke.ultra.common.remote

class SetBearerRequestInterceptor(private val token: () -> String?) : RequestInterceptor {

    override fun intercept(request: RemoteRequest) {
        token()?.let {
            request.header("Authorization", "Bearer $it")
        }
    }
}
