package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.browser.window

/** Retrieves the [DocumentController] from the component's attribute hierarchy. */
val Component<*>.documentCtrl: DocumentController get() = getAttributeRecursive(DocumentController.key)

/**
 * Tracks whether the browser document currently has focus.
 *
 * Listens to visibility-change, focus, and blur events to keep the [hasFocus] stream up to date.
 */
class DocumentController {

    companion object {
        val key = TypedKey<DocumentController>("DocumentController")
    }

    private val hasFocusSource = StreamSource(document.hasFocus())

    /** Stream that emits true when the document has focus, false otherwise. */
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
