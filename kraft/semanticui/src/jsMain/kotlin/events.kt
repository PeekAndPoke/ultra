package io.peekandpoke.kraft.semanticui

import org.w3c.dom.Element
import org.w3c.dom.events.Event

/**
 * True when the event happened on a link (`a[href]`): a component that stops click propagation lets those bubble on,
 * as the router listens on the window for route links (ULTRA-00277).
 */
internal fun Event.isOnLink(): Boolean = (target as? Element)?.closest("a[href]") != null
