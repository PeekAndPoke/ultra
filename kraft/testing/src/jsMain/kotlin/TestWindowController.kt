package io.peekandpoke.kraft.testing

import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.kraft.utils.WindowController
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe

/**
 * A [WindowController] a test drives by hand: `TestBed.preact(appSetup = { windowCtrl(ctrl) }, ...)`, then
 * `ctrl.blur()` and the components see the window lose focus, with no browser event involved.
 */
class TestWindowController(
    hasFocus: Boolean = true,
    isVisible: Boolean = true,
) : WindowController {

    private val hasFocusSource = StreamSource(hasFocus)
    private val isVisibleSource = StreamSource(isVisible)

    private val resizeListeners = mutableListOf<(Vector2D) -> Unit>()
    private val focusListeners = mutableListOf<() -> Unit>()
    private val blurListeners = mutableListOf<() -> Unit>()

    override val hasFocus: Stream<Boolean> = hasFocusSource.readonly

    override val isVisible: Stream<Boolean> = isVisibleSource.readonly

    override fun onWindowResize(callback: (Vector2D) -> Unit): Unsubscribe {
        resizeListeners += callback
        return { resizeListeners.remove(callback) }
    }

    override fun onWindowFocus(callback: () -> Unit): Unsubscribe {
        focusListeners += callback
        return { focusListeners.remove(callback) }
    }

    override fun onWindowBlur(callback: () -> Unit): Unsubscribe {
        blurListeners += callback
        return { blurListeners.remove(callback) }
    }

    /** The window gains focus. */
    fun focus() {
        hasFocusSource(true)
        focusListeners.toList().forEach { it() }
    }

    /** The window loses focus. */
    fun blur() {
        hasFocusSource(false)
        blurListeners.toList().forEach { it() }
    }

    /** The tab is shown ([visible] true) or hidden; a hidden tab has no focus, as in the browser. */
    fun visibility(visible: Boolean) {
        isVisibleSource(visible)
        if (!visible && hasFocusSource()) blur()
    }

    /** The window takes [size]. */
    fun resize(size: Vector2D) {
        resizeListeners.toList().forEach { it(size) }
    }
}
