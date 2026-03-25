package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.StreamSource
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
