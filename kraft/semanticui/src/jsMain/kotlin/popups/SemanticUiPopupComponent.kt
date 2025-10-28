package de.peekandpoke.kraft.semanticui.popups

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.popups.PopupContentRenderer
import de.peekandpoke.kraft.popups.PopupsManager
import de.peekandpoke.kraft.utils.Vector2D
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.semanticui.ui
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

                console.log("dom", it.outerHTML)
                console.log("contentSize", contentSize)
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
                left = pos.x.px
                top = pos.y.px
            }

            props.content(this, props.handle)
        }
    }
}
