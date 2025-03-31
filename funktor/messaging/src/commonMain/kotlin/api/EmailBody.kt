package de.peekandpoke.funktor.messaging.api

import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class EmailBody {

    abstract val content: String

    @Serializable
    @SerialName("text")
    data class Text(override val content: String) : EmailBody()

    @Serializable
    @SerialName("html")
    data class Html(override val content: String) : EmailBody() {
        companion object {
            operator fun invoke(block: HTML.() -> Unit) = Html(
                content = createHTML().html { block() }
            )
        }
    }
}
