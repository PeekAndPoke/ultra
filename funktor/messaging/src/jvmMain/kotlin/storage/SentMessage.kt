package io.peekandpoke.funktor.messaging.storage

import io.peekandpoke.funktor.messaging.api.EmailAttachment
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
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
