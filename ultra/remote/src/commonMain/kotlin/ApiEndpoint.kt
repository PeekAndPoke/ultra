package io.peekandpoke.ultra.remote

/**
 * Sealed hierarchy representing an un-typed API endpoint identified by its [uri].
 *
 * Each subclass corresponds to an HTTP method. Instances are typically used
 * together with [ApiClient] operator functions to issue requests.
 *
 * @see TypedApiEndpoint for the type-safe variant that carries serializer information
 */
sealed class ApiEndpoint {

    /** The URI pattern for the endpoint, may contain `{placeholder}` segments. */
    abstract val uri: String

    /** An HTTP GET endpoint. */
    data class Get(override val uri: String) : ApiEndpoint()

    /** An HTTP PUT endpoint. */
    data class Put(override val uri: String) : ApiEndpoint()

    /** An HTTP POST endpoint. */
    data class Post(override val uri: String) : ApiEndpoint()

    /** An HTTP DELETE endpoint. */
    data class Delete(override val uri: String) : ApiEndpoint()
}
