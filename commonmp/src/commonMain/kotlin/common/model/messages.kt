package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.datetime.PortableDateTime
import kotlinx.serialization.Serializable

typealias Messages = MessageCollection

@Serializable
data class MessageCollection(
    val title: String,
    val messages: List<Message> = emptyList(),
    val children: List<MessageCollection>? = null
) {
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
    val createdAt: PortableDateTime? = null,
) {
    companion object {
        fun info(text: String, createdAt: PortableDateTime? = null) =
            Message(type = Type.info, text = text, createdAt = createdAt)

        fun warning(text: String, createdAt: PortableDateTime? = null) =
            Message(type = Type.warning, text = text, createdAt = createdAt)

        fun error(text: String, createdAt: PortableDateTime? = null) =
            Message(type = Type.error, text = text, createdAt = createdAt)
    }

    @Suppress("EnumEntryName", "Detekt:EnumNaming")
    enum class Type {
        info,
        warning,
        error
    }
}
