package de.peekandpoke.kraft.addons.popups

import de.peekandpoke.kraft.addons.popups.PopupsManager.Handle
import de.peekandpoke.kraft.components.onMouseOut
import de.peekandpoke.kraft.components.onMouseOver
import de.peekandpoke.kraft.components.onMouseUp
import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.kraft.streams.Unsubscribe
import de.peekandpoke.kraft.utils.Rectangle
import de.peekandpoke.kraft.utils.Vector2D
import kotlinx.browser.document
import kotlinx.html.CommonAttributeGroupFacade
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.UIEvent

typealias PopupPositionFn = (target: HTMLElement, contentSize: Vector2D) -> Vector2D

class PopupsManager : Stream<List<Handle>> {
    class ShowHoverPopup(private val popups: PopupsManager) {

        fun show(
            tag: CommonAttributeGroupFacade,
            positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D,
            view: PopupRenderer,
        ) {
            with(tag) {
                var handle: Handle? = null

                val close = {
                    handle?.let { h ->
                        popups.close(h)
                        handle = null
                    }
                }

                onMouseOver { event ->
                    (event.target as? HTMLElement)?.let { target ->
                        if (handle != null) {
                            return@let
                        }

                        popups.add { h ->
                            handle = h

                            PopupComponent(target = target, positioning = positioning, handle = h, content = view)
                        }
                    }
                }
                onMouseOut { close() }
                // In case there is a link, we also close the popup when the link is clicked.
                onMouseUp { close() }
            }
        }
    }

    class Handle internal constructor(
        val id: Int,
        val view: PopupRenderer,
        internal val manager: PopupsManager,
    ) {
        internal val onCloseHandlers = mutableListOf<() -> Unit>()

        /**
         * Adds a listener that is called when the popup gets closed
         */
        fun onClose(onClose: () -> Unit) = apply {
            onCloseHandlers.add(onClose)
        }

        /**
         * Closes the popup
         */
        fun close() = manager.close(this)
    }

    enum class Positioning {
        BottomLeft,
        BottomCenter,
        BottomRight,
    }

    private var handleCounter = 0

    private val streamSource: StreamSource<List<Handle>> = StreamSource(emptyList())

    val showHoverPopup = ShowHoverPopup(popups = this)

    override fun invoke(): List<Handle> = streamSource()

    override fun subscribeToStream(sub: (List<Handle>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    /**
     * Shows a popup relative to the target of the [event] by using the [positioning]
     */
    fun showContextMenu(
        event: UIEvent,
        positioning: Positioning = Positioning.BottomLeft,
        view: PopupRenderer,
    ): Handle {
        event.stopPropagation()
        closeAll()

        val element = event.target as HTMLElement

        return add(element, view) { target, contentSize ->
            val bodyWidth = document.body?.offsetWidth?.toDouble() ?: 1200.0
            val pageCoords = getPageCoords(target)

            val temp = when (positioning) {
                Positioning.BottomLeft -> pageCoords.bottomLeft
                Positioning.BottomCenter -> {
                    ((pageCoords.bottomLeft + pageCoords.bottomRight) / 2.0) - Vector2D(contentSize.x / 2.0, 0.0)
                }

                Positioning.BottomRight -> pageCoords.bottomRight - Vector2D(contentSize.x, 0.0)
            }

//            console.log(pageCoords.width, contentSize.x, pageCoords.width - contentSize.x)

            Vector2D(
                x = maxOf(
                    0.0,
                    minOf(bodyWidth - contentSize.x, temp.x),
                ),
                y = temp.y,
            )
        }
    }

    fun showContextMenu(event: UIEvent, view: PopupRenderer): Handle {
        event.stopPropagation()
        closeAll()

        val element = event.target as HTMLElement
        val moveDown = Vector2D(0.0, 7.0)

        return add(element, view) { target, _ ->

            val mouseEvent: MouseEvent? = event as? MouseEvent

            if (mouseEvent != null) {
                Vector2D(x = mouseEvent.pageX, y = mouseEvent.pageY + 7)
            } else {
                getPageCoords(target).bottomLeft.plus(moveDown)
            }
        }
    }

    internal fun add(element: HTMLElement, content: PopupRenderer, positioning: PopupPositionFn): Handle {
        return add { handle ->
            PopupComponent(
                target = element,
                positioning = positioning,
                handle = handle,
                content = content,
            )
        }
    }

    internal fun add(view: PopupRenderer): Handle {

        return Handle(id = ++handleCounter, view = view, manager = this).apply {
            streamSource.modify { this.plus(this@apply) }
        }
    }

    internal fun close(handle: Handle) {
        // call onClose handlers
        streamSource()
            .filter { it.id == handle.id }
            .forEach { it.onCloseHandlers.forEach { handler -> handler() } }

        // Remove from stack
        streamSource.modify {
            filterNot { it.id == handle.id }
        }
    }

    fun closeAll() {
        streamSource.modify { emptyList() }
    }

    private fun getPageCoords(element: HTMLElement): Rectangle {
        val body = document.body?.getBoundingClientRect() ?: DOMRect()
        val rect = element.getBoundingClientRect()

        return Rectangle(
            x1 = (rect.left - body.left),
            y1 = (rect.top - body.top),
            width = rect.width,
            height = rect.height,
        )
    }
}
