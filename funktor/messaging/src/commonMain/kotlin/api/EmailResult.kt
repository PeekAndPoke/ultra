package de.peekandpoke.funktor.messaging.api

import kotlinx.serialization.Serializable

@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
@Serializable
data class EmailResult internal constructor(
    val success: Boolean,
    val messageId: String? = null,
    val attributes: Map<String, String?> = emptyMap(),
    val error: Map<String, String?>? = null,
) {
    companion object {
        fun ofMessageId(messageId: String) = EmailResult(
            success = true,
            messageId = messageId
        )

        fun ofError(error: Throwable) = EmailResult(
            success = false,
            error = mapOf(
                "message" to error.message,
                "stackTrace" to error.stackTraceToString(),
            )
        )

        fun error(message: String) = EmailResult(
            success = false,
            error = mapOf(
                "message" to message,
            )
        )
    }

    fun <T> withAttribute(key: String, value: T) = copy(
        attributes = attributes.plus(key to value.toString())
    )

    fun <T> withAttributes(vararg pairs: Pair<String, T>) = copy(
        attributes = attributes.plus(
            pairs.map { (k, v) -> k to v.toString() }
        )
    )
}
