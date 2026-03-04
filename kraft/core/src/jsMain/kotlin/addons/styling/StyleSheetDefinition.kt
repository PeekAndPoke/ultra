package de.peekandpoke.kraft.addons.styling

import org.w3c.dom.HTMLElement

interface StyleSheetDefinition {
    fun mount(into: HTMLElement)
    fun unmount()
}
