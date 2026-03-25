package io.peekandpoke.kraft.semanticui.popups

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.popups.PopupContentRenderer
import io.peekandpoke.kraft.popups.PopupsManager
import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.css.Display
import kotlinx.css.Padding
import kotlinx.css.Position
import kotlinx.css.display
import kotlinx.css.left
import kotlinx.css.padding
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.top
import kotlinx.css.zIndex
import kotlinx.html.Tag
import org.w3c.dom.HTMLElement

@Suppress("FunctionName")
fun Tag.SemanticUiPopupComponent(
    target: HTMLElement,
    positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D,
    handle: PopupsManager.Handle,
    content: PopupContentRenderer,
) = comp(
    SemanticUiPopupComponent.Props(
        target = target,
        positioning = positioning,
        handle = handle,
        content = content,
    )
) {
    SemanticUiPopupComponent(it)
}

class SemanticUiPopupComponent(ctx: Ctx<Props>) : Component<SemanticUiPopupComponent.Props>(ctx) {

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

            onUpdate {
                captureContentSize()
            }
        }
    }

    private fun captureContentSize() {
        launch {
            dom?.let {
                val rect = it.getBoundingClientRect()
                contentSize = Vector2D(rect.width, rect.height)

//                console.log("dom", it.outerHTML)
//                console.log("contentSize", contentSize)
            }
        }
    }

    override fun VDom.render() {
        ui.basic.popup {
            css {
                zIndex = 9999
                display = Display.inlineBlock
                padding = Padding(0.px)
                position = Position.absolute

                val pos = props.positioning(props.target, contentSize)
                left = pos.x.toInt().px
                top = pos.y.toInt().px
            }

            props.content(this, props.handle)
        }
    }
}
