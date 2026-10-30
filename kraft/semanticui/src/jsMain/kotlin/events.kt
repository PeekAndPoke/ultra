package io.peekandpoke.kraft.semanticui

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursiveOrNull
import io.peekandpoke.kraft.routing.Router
import org.w3c.dom.Element
import org.w3c.dom.events.Event

/**
 * Stops a click here, as modals and menus do so that the page around them does not react; a click on a link
 * (`a[href]`) goes to the router all the same, which otherwise hears route links on the window (ULTRA-00277).
 */
internal fun Component<*>.stopClick(event: Event) {
    event.stopPropagation()

    if ((event.target as? Element)?.closest("a[href]") != null) {
        getAttributeRecursiveOrNull(Router.key)?.takeLinkClick(event)
    }
}
