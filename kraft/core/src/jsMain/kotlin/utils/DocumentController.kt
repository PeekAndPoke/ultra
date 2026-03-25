package de.peekandpoke.kraft.utils

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.getAttributeRecursive
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.browser.window

val Component<*>.documentCtrl: DocumentController get() = getAttributeRecursive(DocumentController.key)

class DocumentController {

    companion object {
        val key = TypedKey<DocumentController>("DocumentController")
    }

    private val hasFocusSource = StreamSource(document.hasFocus())
    val hasFocus = hasFocusSource.readonly

    init {
        val onFocusChange: (dynamic) -> Unit = {
            hasFocusSource(document.hasFocus())
        }

        document.addEventListener("visibilitychange", onFocusChange)
        window.addEventListener("focus", onFocusChange)
        window.addEventListener("blur", onFocusChange)
    }
}
