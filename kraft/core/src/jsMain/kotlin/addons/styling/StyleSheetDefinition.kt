package io.peekandpoke.kraft.addons.styling

import org.w3c.dom.HTMLElement

/**
 * Defines a style sheet that can be mounted into and unmounted from the DOM.
 */
interface StyleSheetDefinition {
    /**
     * Mounts the style sheet by injecting a `<style>` element into the given [into] element.
     */
    fun mount(into: HTMLElement)

    /** Removes the style sheet from the DOM. */
    fun unmount()
}
