package de.peekandpoke.ktorfx.messaging.storage

import de.peekandpoke.karango.Karango
import de.peekandpoke.ktorfx.messaging.api.EmailAttachment
import de.peekandpoke.ktorfx.messaging.api.EmailResult
import de.peekandpoke.ktorfx.messaging.api.SentMessageModel
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.Timestamped

@Karango
data class SentMessage(
    val result: EmailResult? = null,
    val refs: Set<String>,
    val content: SentMessageModel.Content,
    val tags: Set<String> = emptySet(),
    val attachments: List<EmailAttachment> = emptyList(),
    val lookup: Lookup = Lookup.of(refs, content),
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = MpInstant.Epoch,
) : Timestamped {

    data class Lookup(
        val refs: Set<String>,
    ) {
        companion object {
            fun of(refs: Set<String>, content: SentMessageModel.Content): Lookup {
                val allRefs = refs.plus(
                    when (content) {
                        is SentMessageModel.Content.EmailContent -> content.destination.toAddresses
                    }
                ).filter { it.isNotBlank() }

                return Lookup(
                    refs = allRefs.toSet()
                )
            }
        }
    }

    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}

fun Stored<SentMessage>.asApiModel() = with(value) {
    SentMessageModel(
        id = _id,
        refs = refs,
        tags = tags,
        content = content,
        attachments = attachments,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
