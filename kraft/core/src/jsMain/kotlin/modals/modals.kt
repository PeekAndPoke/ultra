package io.peekandpoke.kraft.modals

import io.peekandpoke.kraft.KraftApp
import kotlinx.html.DIV

/** Renders a modal dialog inside a DIV, receiving the [ModalsManager.Handle] to control closing. */
typealias ModalRenderer = DIV.(ModalsManager.Handle) -> Unit

/** Registers a [DefaultModalsManager] in the Kraft application. */
fun KraftApp.Builder.modals() = apply {
    setAttribute(ModalsManager.key, DefaultModalsManager())
}
