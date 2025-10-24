package de.peekandpoke.kraft.modals

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.KraftDsl
import kotlinx.html.FlowContent

typealias ModalRenderer = FlowContent.(ModalsManager.Handle) -> Unit

@KraftDsl
fun KraftApp.Builder.modals() = apply {
    setAttribute(ModalsManager.key, DefaultModalsManager())
}
