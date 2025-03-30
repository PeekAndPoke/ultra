@file:Suppress(
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
    "Detekt:VariableNaming",
)

package de.peekandpoke.kraft.semanticui

import kotlinx.html.EM
import kotlinx.html.FlowContent
import kotlinx.html.em

class SemanticEmoji(private val parent: FlowContent) {

    operator fun invoke(emoji: String, block: EM.() -> Unit = {}) = render(emoji, block)

    fun render(emoji: String, block: EM.() -> Unit = {}) {

        parent.em {
            attributes["data-emoji"] = emoji
            block()
        }
    }

    @SemanticUiConditionalMarker
    fun given(condition: Boolean, action: SemanticEmoji.() -> SemanticEmoji): SemanticEmoji =
        when (condition) {
            false -> this
            else -> this.action()
        }

    @SemanticUiConditionalMarker
    fun givenNot(condition: Boolean, action: SemanticEmoji.() -> SemanticEmoji): SemanticEmoji =
        given(!condition, action)

    @SemanticUiConditionalMarker
    val then: SemanticEmoji get() = this
}
