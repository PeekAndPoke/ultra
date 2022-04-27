package de.peekandpoke.ultra.semanticui

import kotlinx.html.EM
import kotlinx.html.FlowContent
import kotlinx.html.em

class SemanticEmoji(private val parent: FlowContent) {

    operator fun invoke(emoji: String, block: EM.() -> Unit = {}) = render(emoji, block)

    private fun render(emoji: String, block: EM.() -> Unit) {

        parent.em {
            attributes["data-emoji"] = emoji
            block()
        }
    }
}
