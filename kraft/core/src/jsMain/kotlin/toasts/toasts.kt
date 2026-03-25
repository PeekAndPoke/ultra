package io.peekandpoke.kraft.toasts

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.ultra.common.safeEnumOrNull
import io.peekandpoke.ultra.model.Message
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@KraftDsl
fun KraftApp.Builder.toasts(settings: ToastsManager.Builder.() -> Unit = {}) = apply {
    setAttribute(
        key = ToastsManager.key,
        value = ToastsManager.Builder().apply(settings).build(),
    )
}

@Serializable
data class Toast(
    val type: Message.Type,
    val text: String,
    val duration: Duration?,
) {
    companion object {
        fun of(type: String, text: String, duration: Duration? = 5.seconds): Toast {
            val typeEnum = safeEnumOrNull<Message.Type>(type.lowercase().trim()) ?: Message.Type.info

            return Toast(type = typeEnum, text = text, duration = duration)
        }

        fun of(message: Message, duration: Duration = 5.seconds): Toast {
            return Toast(type = message.type, text = message.text, duration = duration)
        }

        fun info(text: String, duration: Duration? = 5.seconds) =
            Toast(type = Message.Type.info, text = text, duration = duration)

        fun warning(text: String, duration: Duration? = 5.seconds) =
            Toast(type = Message.Type.warning, text = text, duration = duration)

        fun error(text: String, duration: Duration? = 5.seconds) =
            Toast(type = Message.Type.error, text = text, duration = duration)
    }
}
