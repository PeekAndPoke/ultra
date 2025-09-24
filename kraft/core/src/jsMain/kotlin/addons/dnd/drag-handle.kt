package de.peekandpoke.kraft.addons.dnd

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.utils.Vector2D
import de.peekandpoke.kraft.utils.absolutePosition
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.css
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.Display
import kotlinx.css.display
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.reflect.KClass

@Suppress("FunctionName")
inline fun <reified PAYLOAD : Any> Tag.DndDragHandle(
    payload: PAYLOAD,
    host: Component<*>? = null,
    key: String? = null,
) = comp(
    DndDragHandleComponent.Props(
        payloadType = PAYLOAD::class,
        payload = payload,
        host = host,
        key = key
    )
) {
    DndDragHandleComponent(it)
}

class DndDragHandleComponent<PAYLOAD : Any>(ctx: Ctx<Props<PAYLOAD>>) :
    Component<DndDragHandleComponent.Props<PAYLOAD>>(ctx) {

    data class Props<PAYLOAD : Any>(
        val payloadType: KClass<PAYLOAD>,
        val payload: PAYLOAD,
        val host: Component<*>?,
        val key: String?,
    )

    /**
     * The Html Element of the Host Component (the component to be dragged) of the drag handle
     */
    private val hostComponentElement: HTMLElement?
        get() = (props.host ?: parent)?.dom

    /**
     * The Html Element in which the drag handle was placed
     */
    private val handleParentElement: HTMLElement?
        get() = dom?.parentElement as? HTMLElement

    /**
     * While dragging we create a deep clone of the Host Components dom elements
     */
    private var clone: HTMLElement? = null

    /**
     * The initial mouse offset, when dragging was started.
     *
     * This is used to keep the position of the dragged component relative to the mouse cursor position stable.
     */
    private var mouseOffset = Vector2D.zero

    /**
     * Handler for the mouse down event
     */
    private val onMouseDown: (Event) -> Unit by lazy {
        { evt: Event ->

            Unit.apply {

                when (evt.target) {
                    // Prevent dragging when the click was inside of an input element
                    is HTMLInputElement, is HTMLTextAreaElement -> Unit
                    // Otherwise ok
                    else -> window.document.body?.apply {
                        addEventListener("mouseup", onMouseUpOnBody)
                        addEventListener("mousemove", onMouseMoveOnBody)
                    }
                }
            }
        }
    }

    /**
     * Mouse up handler on the stage
     */
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private val onMouseUpOnBody: (Event) -> Unit by lazy {
        { evt: Event ->
            Unit.apply {
                // remove all event listeners from the stage
                window.document.body?.apply {
                    removeEventListener("mouseup", onMouseUpOnBody)
                    removeEventListener("mousemove", onMouseMoveOnBody)
                }

                // Remove the clone from the stage
                clone?.let { clone ->
                    clone.parentElement?.apply { removeChild(clone) }
                }
                // Reset the clone
                clone = null

                // make the text selectable again on the stage
                window.document.body?.style?.setProperty("user-select", "")

                // Signalize that dragging has stopped
                Dnd.onDragEnd()
            }
        }
    }

    /**
     * Mouse move handler on the stage
     */
    private val onMouseMoveOnBody: (Event) -> Unit by lazy {
        { evt: Event ->
            Unit.apply {
                (evt as? MouseEvent)?.let { mouseEvent ->

                    hostComponentElement?.apply {
                        // Did we already create the clone of the draggable element?
                        // If not we need to create it.
                        if (clone == null) {
                            // pick up the offset of the mouse inside of the dragged element
                            val scrollPosition = Vector2D(
                                document.documentElement!!.scrollLeft,
                                document.documentElement!!.scrollTop
                            )

                            mouseOffset = (handleParentElement.absolutePosition() -
                                    hostComponentElement.absolutePosition() -
                                    scrollPosition) +
                                    Vector2D(mouseEvent.offsetX, mouseEvent.offsetY)

                            // create the clone
                            clone = cloneNode(true) as? HTMLElement
                            // apply styles and add the clone to the stage
                            clone?.let { clone ->
                                // TODO: set absolute width and height
                                clone.style.zIndex = "1000"
                                clone.style.setProperty("pointer-events", "none")
                                clone.style.position = "absolute"
                                clone.style.overflowX = "hidden"
                                clone.style.overflowY = "hidden"
                                window.document.body?.appendChild(clone)
                            }
                            // make the text in the whole document not selectable
                            window.document.body?.style?.setProperty("user-select", "none")

                            // Signalize that dragging has started.
                            Dnd.onDragStart(props.payload, props.payloadType)
                        }

                        // On every move, we need to update the position of the dragged item
                        clone?.let { clone ->
                            clone.style.top = "${mouseEvent.clientY - mouseOffset.y}px"
                            clone.style.left = "${mouseEvent.clientX - mouseOffset.x}px"
                        }
                    }
                }
            }
        }
    }

    init {
        lifecycle {
            /**
             * Called when the drag handle component is put into the Dom
             *
             * Here we get our hands on
             * - the Dom of the Host Component
             * - the Dom of the element containing the drag handle
             */
            onMount {
                handleParentElement?.apply {
                    addEventListener("mousedown", onMouseDown)
                }
            }

            /**
             * Removes all event listeners
             */
            onUnmount {
                handleParentElement?.apply {
                    removeEventListener("mousedown", onMouseDown)
                }

                window.document.body?.apply {
                    removeEventListener("mouseup", onMouseUpOnBody)
                    removeEventListener("mousemove", onMouseMoveOnBody)
                }
            }
        }
    }

    /**
     * We need to render a dummy element, so that we can get our hands on
     * - the Dom of the Host Component
     * - the Dom of the element containing the drag handle
     *
     * @see [onMount]
     */
    override fun VDom.render() {
        div {
            css {
                display = Display.none
            }
        }
    }
}
