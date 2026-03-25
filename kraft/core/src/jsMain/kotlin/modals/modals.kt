package io.peekandpoke.kraft.modals

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.KraftDsl
import kotlinx.html.DIV

typealias ModalRenderer = DIV.(ModalsManager.Handle) -> Unit

@KraftDsl
fun KraftApp.Builder.modals() = apply {
    setAttribute(ModalsManager.key, DefaultModalsManager())
}
