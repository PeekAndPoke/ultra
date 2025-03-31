package de.peekandpoke.kraft.vdom

import de.peekandpoke.kraft.components.Component
import org.w3c.dom.HTMLElement

interface VDomEngine {

    data class Options(
        val debugMode: Boolean = false,
    ) {
        companion object {
            val default = Options()
        }
    }

    val options: Options

    fun mount(element: HTMLElement, view: VDom.() -> Any?)

    fun createTagConsumer(host: Component<*>?): VDomTagConsumer

    fun triggerRedraw(component: Component<*>)

    fun render(host: Component<*>? = null, builder: VDom.() -> Any?): dynamic {
        return VDom(this, host).render { builder() }
    }
}
