package io.peekandpoke.kraft.popups

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import kotlinx.css.Display
import kotlinx.css.Padding
import kotlinx.css.display
import kotlinx.css.left
import kotlinx.css.padding
import kotlinx.css.px
import kotlinx.css.top
import kotlinx.css.zIndex
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.HTMLElement

/** Renders a [PopupComponent] positioned relative to a [target] element. */
@Suppress("FunctionName")
fun Tag.PopupComponent(
    target: HTMLElement,
    positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D,
    handle: PopupsManager.Handle,
    content: PopupContentRenderer,
) = comp(
    PopupComponent.Props(
        target = target,
        positioning = positioning,
        handle = handle,
        content = content,
    )
) {
    PopupComponent(it)
}

/** Default popup component that positions itself using a [Props.positioning] function. */
class PopupComponent(ctx: Ctx<Props>) : Component<PopupComponent.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val target: HTMLElement,
        val positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D,
        val handle: PopupsManager.Handle,
        val content: PopupContentRenderer,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var contentSize: Vector2D by value(Vector2D.zero)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                captureContentSize()
            }
        }
    }

    private fun captureContentSize() {
        dom?.let {
            val bounds = it.getBoundingClientRect()
            contentSize = Vector2D(bounds.width, bounds.height)
        }
    }

    override fun VDom.render() {

        console.log("Rending Default popup component ${props.handle.id}")

        captureContentSize()

        // Just a container
        div {
            css {
                zIndex = 9999
                display = Display.inlineBlock
                padding = Padding(0.px)

                val pos = props.positioning(props.target, contentSize)
                left = pos.x.px
                top = pos.y.px
            }

            props.content(this, props.handle)
        }
    }
}
