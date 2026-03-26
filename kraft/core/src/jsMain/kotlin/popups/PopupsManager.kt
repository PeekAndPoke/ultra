package io.peekandpoke.kraft.popups

import io.peekandpoke.kraft.components.AutoMountedUi
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.kraft.utils.getPageCoords
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.html.onMouseOut
import io.peekandpoke.ultra.html.onMouseOver
import io.peekandpoke.ultra.html.onMouseUp
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.FlowContent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.UIEvent

/**
 * A manager for popups.
 */
class PopupsManager(
    val settings: Settings,
) : Stream<List<PopupsManager.Handle>>, AutoMountedUi {
    companion object {
        val key = TypedKey<PopupsManager>("popups")

        /** Retrieves the [PopupsManager] from the component tree. */
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
                Positioning.TopLeft, Positioning.BottomLeft -> {
                    pageCoords.topLeft
                }

                Positioning.TopCenter, Positioning.BottomCenter -> {
                    (pageCoords.topLeft + pageCoords.topRight) / 2.0
                }

                Positioning.TopRight, Positioning.BottomRight -> {
                    pageCoords.topRight
                }
            }

            calculatePopupPosition(
                anchor = anchor,
                positioning = positioning,
                contentSize = contentSize,
                elementSize = pageCoords.dimension,
            )
        }
    }

    /** Shows a context menu popup anchored to the [event] target with bottom-left positioning. */
    fun showContextMenu(event: UIEvent, view: PopupContentRenderer): Handle {
        return showContextMenu(
            event = event,
            positioning = Positioning.BottomLeft,
            view = view,
        )
    }

    /** Shows a context menu popup at the given [anchor] position with the specified [positioning]. */
    fun showContextMenu(
        anchor: Vector2D,
        positioning: Positioning,
        view: PopupContentRenderer,
    ): Handle {
        closeAll()

        val element = document.body as HTMLElement

        return add(element, view) { _, contentSize ->
            calculatePopupPosition(
                anchor = anchor,
                positioning = positioning,
                contentSize = contentSize,
            )
        }
    }

    private fun calculatePopupPosition(
        anchor: Vector2D,
        positioning: Positioning,
        contentSize: Vector2D,
        elementSize: Vector2D = Vector2D.zero,
    ): Vector2D {
        val bodyWidth = document.body?.offsetWidth?.toDouble() ?: 1200.0
        val bodyHeight = kotlinx.browser.window.innerHeight.toDouble()

        // Calculate the initial position based on the requested positioning
        var temp = when (positioning) {
            Positioning.TopLeft -> anchor - Vector2D(0.0, contentSize.y)
            Positioning.TopCenter -> anchor - Vector2D(contentSize.x / 2.0, contentSize.y)
            Positioning.TopRight -> anchor - Vector2D(contentSize.x, contentSize.y)
            Positioning.BottomLeft -> anchor + Vector2D(0.0, elementSize.y)
            Positioning.BottomCenter -> anchor + Vector2D(-contentSize.x / 2.0, elementSize.y)
            Positioning.BottomRight -> anchor + Vector2D(-contentSize.x, elementSize.y)
        }

        // Fallback for Top -> Bottom if not enough space on top
        if (temp.y < 0 && anchor.y + elementSize.y + contentSize.y <= bodyHeight) {
            temp = Vector2D(temp.x, anchor.y + elementSize.y)
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

    /** Closes all open popups. */
    fun closeAll() {
        streamSource.modify { emptyList() }
    }

    /** Adds a popup anchored to [element] with the given [positioning] function and [content]. */
    fun add(element: HTMLElement, content: PopupContentRenderer, positioning: PopupPositionFn): Handle {
        return add { handle ->
            settings.popupRenderer(this, element, positioning, handle, content)
        }
    }

    /** Adds a popup with the given content [view] and returns a handle. */
    fun add(view: PopupContentRenderer): Handle {
        return Handle(id = ++handleCounter, view = view, manager = this).also {
            streamSource.modify { plus(it) }
        }
    }

    /** Closes the popup identified by the given [handle] and invokes its onClose handlers. */
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
