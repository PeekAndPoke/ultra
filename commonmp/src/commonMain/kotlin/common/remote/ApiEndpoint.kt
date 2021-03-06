package de.peekandpoke.ultra.common.remote

sealed class ApiEndpoint {

    abstract val uri: String

    data class Get(override val uri: String) : ApiEndpoint()

    data class Put(override val uri: String) : ApiEndpoint()

    data class Post(override val uri: String) : ApiEndpoint()

    data class Delete(override val uri: String) : ApiEndpoint()
}
