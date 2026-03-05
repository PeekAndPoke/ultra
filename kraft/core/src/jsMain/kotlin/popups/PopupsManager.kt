package de.peekandpoke.kraft.popups

import de.peekandpoke.kraft.components.AutoMountedUi
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.getAttributeRecursive
import de.peekandpoke.kraft.utils.Vector2D
import de.peekandpoke.kraft.utils.getPageCoords
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.html.onMouseOut
import de.peekandpoke.ultra.html.onMouseOver
import de.peekandpoke.ultra.html.onMouseUp
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.FlowContent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.UIEvent

/** A function which renders the content of a popup */
typealias PopupContentRenderer = FlowContent.(handle: PopupsManager.Handle) -> Unit

/** A function which positions a popup */
typealias PopupPositionFn = (target: HTMLElement, contentSize: Vector2D) -> Vector2D

/** A function which renders a popup */
typealias PopupComponentFactory = FlowContent.(
    target: HTMLElement,
    positioning: PopupPositionFn,
    handle: PopupsManager.Handle,
    content: PopupContentRenderer,
) -> Unit

/**
 * A manager for popups.
 */
class PopupsManager(
    val settings: Settings,
) : Stream<List<PopupsManager.Handle>>, AutoMountedUi {
    companion object {
        val key = TypedKey<PopupsManager>("popups")

        val Component<*>.popups: PopupsManager get() = getAttributeRecursive(key)

        val DefaultPopupFactory: PopupComponentFactory = { target, positioning, handle, content ->
            PopupComponent(target = target, positioning = positioning, handle = handle, content = content)
        }
    }

    /**
     * Settings for the popups manager
     */
    data class Settings(
        val popupRenderer: PopupComponentFactory,
    )

    /**
     * PopupsManager builder
     */
    class Builder internal constructor() {
        private var popupFactory: PopupComponentFactory = DefaultPopupFactory

        fun popupFactory(factory: PopupComponentFactory) {
            popupFactory = factory
        }

        internal fun build() = PopupsManager(
            Settings(
                popupRenderer = popupFactory,
            )
        )
    }

    /**
     * A helper class to show a popup when the mouse hovers over an element.
     */
    class ShowHoverPopup(private val popups: PopupsManager) {

        fun show(
            tag: CommonAttributeGroupFacade,
            positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D,
            view: PopupContentRenderer,
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
                    val element = event.currentTarget as? HTMLElement
                        ?: event.target as? HTMLElement

                    element?.let { target ->
                        if (handle != null) {
                            return@let
                        }

                        popups.add { h ->
                            handle = h

                            popups.settings.popupRenderer(this, target, positioning, h, view)
                        }
                    }
                }
                onMouseOut { close() }
                // In case there is a link, we also close the popup when the link is clicked.
                onMouseUp { close() }
            }
        }
    }

    /**
     * A handle to a popup.
     */
    class Handle internal constructor(
        val id: Int,
        val view: PopupContentRenderer,
        internal val manager: PopupsManager,
    ) {
        internal val onCloseHandlers = mutableListOf<() -> Unit>()

        /**
         * Adds a listener which is called when the popup gets closed
         */
        fun onClose(onClose: () -> Unit) = apply {
            onCloseHandlers.add(onClose)
        }

        /**
         * Closes the popup
         */
        fun close() = manager.close(this)
    }

    /**
     * Enum representing the positioning options for a popup.
     */
    enum class Positioning {
        TopLeft,
        TopCenter,
        TopRight,
        BottomLeft,
        BottomCenter,
        BottomRight,
    }

    /** The current list of popups */
    private var handleCounter = 0

    /** The current list of popups */
    private val streamSource: StreamSource<List<Handle>> = StreamSource(emptyList())

    /** Helper for displaying hover popups */
    val showHoverPopup = ShowHoverPopup(popups = this)

    /** @inheritDoc */
    override fun invoke(): List<Handle> = streamSource()

    /** @inheritDoc */
    override fun subscribeToStream(sub: (List<Handle>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    /** auto mount priority */
    override val autoMountPriority = 1000

    /**
     * Mounts the popup stage to the given [element]
     */
    override fun autoMount(element: FlowContent) {
        with(element) {
            PopupsStage(popups = this@PopupsManager)
        }
    }

    /**
     * Shows a popup relative to the target of the [event] by using the [positioning]
     */
    fun showContextMenu(
        event: UIEvent,
        positioning: Positioning = Positioning.BottomLeft,
        view: PopupContentRenderer,
    ): Handle {
        event.stopPropagation()
        closeAll()

        val element = event.currentTarget as? HTMLElement
            ?: event.target as HTMLElement

        return add(element, view) { target, contentSize ->
            val pageCoords = target.getPageCoords()

            val anchor = when (positioning) {
                Positioning.TopLeft -> {
                    pageCoords.topLeft
                }

                Positioning.TopCenter -> {
                    (pageCoords.topLeft + pageCoords.topRight) / 2.0
                }

                Positioning.TopRight -> {
                    pageCoords.topRight
                }

                Positioning.BottomLeft -> {
                    pageCoords.bottomLeft
                }

                Positioning.BottomCenter -> {
                    (pageCoords.bottomLeft + pageCoords.bottomRight) / 2.0
                }

                Positioning.BottomRight -> {
                    pageCoords.bottomRight
                }
            }

            calculatePopupPosition(anchor, positioning, contentSize)
        }
    }

    fun showContextMenu(event: UIEvent, view: PopupContentRenderer): Handle {
        event.stopPropagation()
        closeAll()

        val element = event.currentTarget as? HTMLElement
            ?: event.target as HTMLElement

        val moveDown = Vector2D(0.0, 7.0)

        return add(element, view) { target, _ ->

            val mouseEvent: MouseEvent? = event as? MouseEvent

            if (mouseEvent != null) {
                Vector2D(x = mouseEvent.pageX, y = mouseEvent.pageY + 7)
            } else {
                target.getPageCoords().bottomLeft.plus(moveDown)
            }
        }
    }

    fun showContextMenu(
        anchor: Vector2D,
        positioning: Positioning,
        view: PopupContentRenderer,
    ): Handle {
        closeAll()

        val element = document.body as HTMLElement

        return add(element, view) { _, contentSize ->
            calculatePopupPosition(anchor, positioning, contentSize)
        }
    }

    private fun calculatePopupPosition(
        anchor: Vector2D,
        positioning: Positioning,
        contentSize: Vector2D,
    ): Vector2D {
        val bodyWidth = document.body?.offsetWidth?.toDouble() ?: 1200.0
        val bodyHeight = kotlinx.browser.window.innerHeight.toDouble()

        // Calculate the initial position based on the requested positioning
        var temp = when (positioning) {
            Positioning.TopLeft -> anchor - Vector2D(0.0, contentSize.y)
            Positioning.TopCenter -> anchor - Vector2D(contentSize.x / 2.0, contentSize.y)
            Positioning.TopRight -> anchor - Vector2D(contentSize.x, contentSize.y)
            Positioning.BottomLeft -> anchor
            Positioning.BottomCenter -> anchor - Vector2D(contentSize.x / 2.0, 0.0)
            Positioning.BottomRight -> anchor - Vector2D(contentSize.x, 0.0)
        }

        // Fallback for Top -> Bottom if not enough space on top
        if (temp.y < 0 && anchor.y + contentSize.y <= bodyHeight) {
            temp = Vector2D(temp.x, anchor.y)
        }
        // Fallback for Bottom -> Top if not enough space on bottom
        else if (temp.y + contentSize.y > bodyHeight && anchor.y - contentSize.y >= 0) {
            temp = Vector2D(temp.x, anchor.y - contentSize.y)
        }

        // Fallback for Left/Right boundary collisions
        // If it doesn't fit on the left, align it to the left edge
        if (temp.x < 0) {
            temp = Vector2D(0.0, temp.y)
        }
        // If it doesn't fit on the right, align it to the right edge
        else if (temp.x + contentSize.x > bodyWidth) {
            temp = Vector2D(maxOf(0.0, bodyWidth - contentSize.x), temp.y)
        }

        return temp
    }

    fun closeAll() {
        streamSource.modify { emptyList() }
    }

    fun add(element: HTMLElement, content: PopupContentRenderer, positioning: PopupPositionFn): Handle {
        return add { handle ->
            settings.popupRenderer(this, element, positioning, handle, content)
        }
    }

    fun add(view: PopupContentRenderer): Handle {
        return Handle(id = ++handleCounter, view = view, manager = this).also {
            streamSource.modify { plus(it) }
        }
    }

    fun close(handle: Handle) {
        // call onClose handlers
        streamSource()
            .filter { it.id == handle.id }
            .forEach { it.onCloseHandlers.forEach { handler -> handler() } }

        // Remove from stack
        streamSource.modify {
            filterNot { it.id == handle.id }
        }
    }
}
