package io.peekandpoke.kraft.popups

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.debugId
import io.peekandpoke.ultra.html.key
import kotlinx.browser.document
import kotlinx.css.Position
import kotlinx.css.left
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.top
import kotlinx.css.zIndex
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.events.Event

/** Renders the [PopupsStage] component that displays all active popups. */
@Suppress("FunctionName")
fun Tag.PopupsStage(
    popups: PopupsManager,
) = comp(
    PopupsStage.Props(popups = popups)
) {
    PopupsStage(it)
}

/** Component that subscribes to a [PopupsManager] and renders all open popups. Closes all on document click. */
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
                position = Position.fixed
                top = 0.px
                left = 0.px
                zIndex = 10000
            }

            current.forEach {
                div {
                    key = "popup-${it.id}"
                    debugId("popup-${it.id}")

                    css {
                        position = Position.absolute
                        top = 0.px
                        left = 0.px
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
