package de.peekandpoke.kraft.addons.popups

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.css
import kotlinx.browser.document
import kotlinx.css.Display
import kotlinx.css.Position
import kotlinx.css.display
import kotlinx.css.position
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.events.Event

@Suppress("FunctionName")
fun Tag.PopupsStage(
    popups: PopupsManager,
) = comp(
    PopupsStage.Props(popups = popups)
) {
    PopupsStage(it)
}

class PopupsStage(ctx: Ctx<Props>) : Component<PopupsStage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val popups: PopupsManager,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val current: List<PopupsManager.Handle> by subscribingTo(props.popups)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                document.addEventListener("click", ::closeAllListener)
                document.addEventListener("contextmenu", ::closeAllListener)
            }

            onUnmount {
                document.removeEventListener("click", ::closeAllListener)
                document.removeEventListener("contextmenu", ::closeAllListener)
            }
        }
    }

    override fun VDom.render() {
        div(classes = "popup-stage") {
            css {
                position = Position.absolute
            }
//            key = current.hashCode().toString()

//            console.log("ModelDialogStage", current.hashCode().toString(), current.size)

            current.forEach {
                div {
                    key = "popup-${it.id}"
                    css {
                        display = Display.inlineBlock
                    }
                    it.view(this, it)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun closeAllListener(evt: Event) {
        props.popups.closeAll()
    }
}
