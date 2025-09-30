package de.peekandpoke.kraft.vdom

import de.peekandpoke.kraft.KraftApp
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

    fun mount(app: KraftApp, element: HTMLElement, view: VDom.() -> Any?)

    fun createTagConsumer(host: Component<*>?): VDomTagConsumer

    fun triggerRedraw(component: Component<*>)

    fun render(component: Component<*>): dynamic {
        val vdom = VDom(
            consumer = createTagConsumer(component),
            component = component,
        )

        return vdom.render {
            with(component) {
                vdom.render()
            }
        }
    }
}
