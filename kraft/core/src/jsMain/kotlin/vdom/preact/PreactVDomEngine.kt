package de.peekandpoke.kraft.vdom.preact

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.kraft.vdom.VDomTagConsumer
import org.w3c.dom.HTMLElement
import de.peekandpoke.kraft.components.Component as KraftComponent
import preact.Component as PreactComponent

class PreactVDomEngine(
    override val options: VDomEngine.Options = VDomEngine.Options.default,
) : VDomEngine {

    override fun mount(app: KraftApp, element: HTMLElement, view: VDom.() -> Any?) {

        val root = VDom.Root(
            ctx = Ctx(
                engine = this,
                parent = null,
                props = VDom.Root.Props(
                    app = app,
                    view = view,
                )
            ),
        )

        val lowLevelRoot = render(root)

//        console.log("preact lowLevelRoot", lowLevelRoot)

        preact.render(lowLevelRoot, element)
    }

    override fun createTagConsumer(host: KraftComponent<*>): VDomTagConsumer {
        return PreactTagConsumer(this, host)
    }

    override fun triggerRedraw(component: KraftComponent<*>) {
        (component.lowLevelBridgeComponent as? PreactComponent)?.setState(jsObject())
    }
}
