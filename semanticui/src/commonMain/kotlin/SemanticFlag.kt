package de.peekandpoke.ultra.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.I
import kotlinx.html.i

class SemanticFlag(private val parent: FlowContent) {

    operator fun invoke(flag: String, block: I.() -> Unit = {}) = render(flag, block)

    fun render(flag: String, block: I.() -> Unit = {}) {

        parent.i("$flag flag") {
            block()
        }
    }

    @SemanticUiConditionalMarker
    fun given(condition: Boolean, action: SemanticFlag.() -> SemanticFlag): SemanticFlag =
        when (condition) {
            false -> this
            else -> this.action()
        }

    @SemanticUiConditionalMarker
    fun givenNot(condition: Boolean, action: SemanticFlag.() -> SemanticFlag): SemanticFlag =
        given(!condition, action)

    @SemanticUiConditionalMarker
    val then: SemanticFlag get() = this
}
