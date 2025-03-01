package de.peekandpoke.ktorfx.rest

import de.peekandpoke.ultra.common.model.Message
import kotlinx.serialization.Serializable

/**
 * Standard format for an api response
 */
@Serializable
data class ApiResponse<out T>(
    /** The status code */
    val status: HttpStatusCode,
    /** The response data */
    val data: T?,
    /** Messages to be sent along */
    val messages: List<Message>? = null,
    /** Insights */
    val insights: Insights? = null,
) {
    @Serializable
    data class Insights(
        val ts: Long,
        val method: String,
        val url: String,
        val server: String,
        val status: HttpStatusCode,
        val durationMs: Double?,
        val detailsUri: String?,
    )

    companion object {
        /**
         * Creates an [ApiResponse] with [HttpStatusCode.OK] and the given [data]
         */
        fun <T> ok(): ApiResponse<T> = ApiResponse(HttpStatusCode.OK, null)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.OK] and the given [data]
         */
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.OK, data)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.OK] and the given [data]
         */
        fun <T> created(data: T): ApiResponse<T> = ApiResponse(HttpStatusCode.Created, data)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.Created] or [HttpStatusCode.OK]
         */
        fun <T> okOrCreated(created: Boolean, data: T) = when (created) {
            true -> created(data)
            false -> ok(data)
        }

        /**
         * Create an [ApiResponse] with [HttpStatusCode.OK] when the data is not null.
         *
         * Otherwise creates an [ApiResponse] with [HttpStatusCode.NotFound].
         */
        fun <T> okOrNotFound(data: T?): ApiResponse<T> = when (data) {
            null -> notFound()
            else -> ok(data)
        }

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.Conflict]
         */
        fun <T> badRequest(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.BadRequest, data)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.Conflict]
         */
        fun <T> conflict(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Conflict, data)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.Forbidden]
         */
        fun <T> forbidden(data: T? = null): ApiResponse<T> = ApiResponse(HttpStatusCode.Forbidden, data)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.InternalServerError]
         */
        fun <T> internalServerError(data: T? = null): ApiResponse<T> =
            ApiResponse(HttpStatusCode.InternalServerError, data)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.NotFound]
         */
        fun <T> notFound(): ApiResponse<T> = ApiResponse(HttpStatusCode.NotFound, null)

        /**
         * Creates an [ApiResponse] with [HttpStatusCode.Unauthorized]
         */
        fun <T> unauthorized(): ApiResponse<T> = ApiResponse(HttpStatusCode.Unauthorized, null)
    }

    /** Returns a new [ApiResponse] with the given [insights] set */
    fun withInsights(insights: Insights) = copy(insights = insights)

    /** Returns a new [ApiResponse] while adding messages of type "info" */
    fun withInfo(vararg text: String): ApiResponse<T> = withMessages(Message.Type.info, text)

    /** Returns a new [ApiResponse] while adding messages of type "warning" */
    fun withWarning(vararg text: String): ApiResponse<T> = withMessages(Message.Type.warning, text)

    /** Returns a new [ApiResponse] while adding messages of type "error" */
    fun withError(vararg text: String): ApiResponse<T> = withMessages(Message.Type.error, text)

    /**
     * Returns 'true' when the request was successful, meaning 200 <= status code <= 299
     */
    fun isSuccess(): Boolean {
        return status.isSuccess()
    }

    /**
     * Returns 'true' when the request was not successful,
     */
    fun isNotSuccess(): Boolean {
        return !isSuccess()
    }

    /**
     * Creates a new ApiResponse, while mapping the [data].
     */
    fun <R> map(mapper: (T) -> R): ApiResponse<R> {
        return ApiResponse(
            status = status,
            data = data?.let { mapper(it) },
            messages = messages,
            insights = insights,
        )
    }

    /**
     * Internal helper for adding messages
     */
    private fun withMessages(type: Message.Type, texts: Array<out String>): ApiResponse<T> = copy(
        messages = (messages ?: emptyList()).plus(
            texts.map { Message(type, it) }
        )
    )
}

