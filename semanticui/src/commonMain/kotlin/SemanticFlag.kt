package de.peekandpoke.ultra.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.I
import kotlinx.html.i

class SemanticFlag(private val parent: FlowContent) {

    operator fun invoke(flag: String, block: I.() -> Unit = {}) = render(flag, block)

    private fun render(flag: String, block: I.() -> Unit = {}) {

        parent.i("$flag flag") {
            block()
        }
    }
}
