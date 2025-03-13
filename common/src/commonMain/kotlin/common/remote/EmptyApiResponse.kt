package de.peekandpoke.ultra.common.remote

import de.peekandpoke.ultra.common.model.Message
import kotlinx.serialization.Serializable

/**
 * This class is used by the interceptors to extract the messages.
 */
@Serializable
data class EmptyApiResponse(
    /** The status code */
    val status: HttpStatusCode,
    /** Messages to be sent along */
    val messages: List<Message>? = null,
    /** Insights head */
    val insights: ApiResponse.Insights? = null,
)
