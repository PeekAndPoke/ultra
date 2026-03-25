package io.peekandpoke.kraft.popups

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.utils.Vector2D
import kotlinx.html.FlowContent
import org.w3c.dom.HTMLElement

/** A function which renders the content of a popup */
typealias PopupContentRenderer = FlowContent.(handle: PopupsManager.Handle) -> Unit

/** A function which positions a popup */
typealias PopupPositionFn = (target: HTMLElement, contentSize: Vector2D) -> Vector2D

/** A function which renders a popup */
typealias PopupComponentFactory = FlowContent.(
    target: HTMLElement,
    positioning: PopupPositionFn,
    handle: PopupsManager.Handle,
    content: PopupContentRenderer,
) -> Unit

/**
 * Register a [PopupsManager] in the [KraftApp]
 */
@KraftDsl
fun KraftApp.Builder.popups(settings: PopupsManager.Builder.() -> Unit = {}) = apply {
    setAttribute(
        key = PopupsManager.key,
        value = PopupsManager.Builder().apply(settings).build(),
    )
}
