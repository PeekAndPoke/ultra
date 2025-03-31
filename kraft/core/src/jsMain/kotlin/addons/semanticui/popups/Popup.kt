package de.peekandpoke.kraft.addons.semanticui.popups

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.Display
import kotlinx.css.display
import kotlinx.html.DIV
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

@Suppress("FunctionName")
fun Tag.Popup(
    content: DIV.() -> Unit,
) = comp(
    Popup.Props(content = content)
) {
    Popup(it)
}

class Popup(ctx: Ctx<Props>) : Component<Popup.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val content: DIV.() -> Unit,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var host: HTMLElement? by value(null)

    private var show by value(false)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                host = dom?.parentElement as HTMLElement

                console.log("Popup host", host)

                host?.addEventListener("mouseover", onOver)
                host?.addEventListener("mouseout", onOut)
            }

            onUnmount {
                host?.removeEventListener("mouseover", onOver)
                host?.removeEventListener("mouseout", onOut)
            }
        }
    }

    private val onOver = { _: Event ->
        show = true
    }

    private val onOut = { _: Event ->
        show = false
    }

    override fun VDom.render() {

        if (show) {
            ui.popup.top.left {
                props.content(this)
            }
        } else {
            // Just a div to get our hands on the dom of the parent dom element
            div {
                css { display = Display.none }
            }
        }
    }
}
