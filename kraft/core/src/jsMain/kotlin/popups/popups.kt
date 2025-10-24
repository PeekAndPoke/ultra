package de.peekandpoke.kraft.popups

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.KraftDsl
import kotlinx.html.FlowContent

typealias PopupContentRenderer = FlowContent.(PopupsManager.Handle) -> Unit

@KraftDsl
fun KraftApp.Builder.popups(settings: PopupsManager.Builder.() -> Unit = {}) = apply {
    setAttribute(
        key = PopupsManager.key,
        value = PopupsManager.Builder().apply(settings).build(),
    )
}
