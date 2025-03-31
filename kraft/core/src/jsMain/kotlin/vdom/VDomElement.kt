package de.peekandpoke.kraft.vdom

import kotlinx.html.org.w3c.dom.events.Event

interface VDomElement {

    fun appendChild(child: VDomElement) {}

    fun addEvent(name: String, callback: (Event) -> Any?) {}

    fun render(): dynamic
}
