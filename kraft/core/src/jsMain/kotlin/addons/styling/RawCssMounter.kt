package de.peekandpoke.kraft.addons.styling

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement

internal class RawCssMounter(private val name: String? = null, private val content: () -> String) {
    private var mounted: HTMLStyleElement? = null

    fun mount(into: HTMLElement) {
        if (mounted == null) {
            val css = content()

            mounted = document.createElement("style") as HTMLStyleElement
            mounted!!.setAttribute("id", "injected-${css.hashCode()}")
            mounted!!.textContent = css

            name?.let {
                mounted!!.setAttribute("data-name", it)
            }

//            console.log("body", body)
//            console.log("style", styleNode)

            into.appendChild(mounted!!)
        }
    }

    fun unmount() {
        mounted?.let {
            it.remove()
            mounted = null
        }
    }
}
