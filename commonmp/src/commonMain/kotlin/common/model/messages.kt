package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

typealias Messages = MessageCollection

// TODO: test me
data class ResultWithMessages<T>(
    val messages: Messages,
    val result: T,
)

// TODO: test me
fun <T> withMessages(title: String = "", block: MessageCollection.Builder.() -> T): ResultWithMessages<T> {
    val builder = MessageCollection.Builder(title = title)
    val result: T = builder.run(block)

    return ResultWithMessages(
        messages = builder.build(),
        result = result,
    )
}

@Serializable
data class MessageCollection(
    val title: String,
    val messages: List<Message> = emptyList(),
    val children: List<MessageCollection>? = null
) {
    /**
     * Helps building [Messages]
     */
    // TODO: test me
    class Builder internal constructor(var title: String = "") {

        private val messages: MutableList<Message> = mutableListOf()

        internal fun build() = Messages(
            title = title,
            messages = messages,
            children = emptyList(),
        )

        /**
         * Creates a copy by adding the given [message]
         */
        fun addMessage(vararg message: Message) {
            messages.addAll(message)
        }

        fun addInfo(text: String, ts: MpInstant? = null) {
            addMessage(
                Message(type = Message.Type.info, text = text, ts = ts)
            )
        }

        fun addWarning(text: String, createdAt: MpInstant? = null) {
            addMessage(
                Message(type = Message.Type.warning, text = text, ts = createdAt)
            )
        }

        fun addError(text: String, createdAt: MpInstant? = null) {
            addMessage(
                Message(type = Message.Type.error, text = text, ts = createdAt)
            )
        }
    }

    /**
     * Creates a copy by adding the given [message]
     */
    fun add(vararg message: Message) = copy(
        messages = this.messages.plus(message)
    )

    /**
     * Get all messages recursively.
     *
     * Includes:
     *  - the [messages]
     *  - and all message of all [children]
     *
     */
    fun getAllMessages(): List<Message> {

        return mutableListOf<Message>()
            .apply { addAll(messages) }
            .apply { children?.let { children -> addAll(children.flatMap { it.getAllMessages() }) } }
            .toList()
    }

    /**
     * The type of the worst message in the entire collection.
     *
     * Taking into account:
     * - the [messages]
     * - the messages of all [children]
     */
    fun getWorstMessageType(): Message.Type {

        return getAllMessages()
            .map { it.type }
            .toSet()
            .maxOrNull()
            ?: Message.Type.info
    }

    /**
     * Returns 'true' when none of the messages is a [Message.Type.warning] or [Message.Type.error]
     */
    val isSuccess get() = (getWorstMessageType() == Message.Type.info)

    /**
     * Returns 'true' when none of the messages is a [Message.Type.error]
     */
    val isWarningOrBetter get() = getWorstMessageType() in listOf(Message.Type.warning, Message.Type.info)

    /**
     * Returns 'true' when at least one the messages is a [Message.Type.error]
     */
    val isError get() = getWorstMessageType() == Message.Type.error
}

@Serializable
data class Message(
    val type: Type,
    val text: String,
    val ts: MpInstant? = null,
) {
    companion object {
        fun info(text: String, ts: MpInstant? = null) =
            Message(type = Type.info, text = text, ts = ts)

        fun warning(text: String, createdAt: MpInstant? = null) =
            Message(type = Type.warning, text = text, ts = createdAt)

        fun error(text: String, createdAt: MpInstant? = null) =
            Message(type = Type.error, text = text, ts = createdAt)
    }

    @Suppress("EnumEntryName", "Detekt:EnumNaming")
    enum class Type {
        info,
        warning,
        error
    }
}
