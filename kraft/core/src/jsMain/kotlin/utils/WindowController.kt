package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

/** Retrieves the [WindowController] from the component's attribute hierarchy. */
val Component<*>.windowCtrl: WindowController get() = getAttributeRecursive(WindowController.key)

/**
 * Subscribes to browser window resize events via the app's [WindowController].
 *
 * The callback receives the current window inner size as a [Vector2D].
 * The subscription is automatically removed when the component unmounts.
 */
fun <T> Component.LifeCycle<T>.onWindowResize(callback: (Vector2D) -> Unit): Unsubscribe {
    val unsub = component.windowCtrl.onWindowResize(callback)
    onUnmount { unsub() }
    return unsub
}

/**
 * Subscribes to browser window focus events via the app's [WindowController].
 *
 * Fires when the browser window / tab gains focus (e.g. user switches back to it).
 * The subscription is automatically removed when the component unmounts.
 */
fun <T> Component.LifeCycle<T>.onWindowFocus(callback: () -> Unit): Unsubscribe {
    val unsub = component.windowCtrl.onWindowFocus(callback)
    onUnmount { unsub() }
    return unsub
}

/**
 * Subscribes to browser window blur events via the app's [WindowController].
 *
 * Fires when the browser window / tab loses focus (e.g. user switches to another tab).
 * The subscription is automatically removed when the component unmounts.
 */
fun <T> Component.LifeCycle<T>.onWindowBlur(callback: () -> Unit): Unsubscribe {
    val unsub = component.windowCtrl.onWindowBlur(callback)
    onUnmount { unsub() }
    return unsub
}

/**
 * App-level controller tracking browser document state.
 *
 * Exposes:
 * - [hasFocus] — stream of document focus state (visibilitychange, window focus/blur)
 * - [isVisible] — stream of the page's visibility (`document.visibilityState`): false for a hidden tab
 * - [onWindowResize] — subscribe to browser window resize events
 * - [onWindowFocus] / [onWindowBlur] — subscribe to window focus/blur events
 *
 * The state is the browser's, so one instance serves every app on the page: [default], registered in every
 * [io.peekandpoke.kraft.KraftApp] and readable outside components. A test passes its own implementation via
 * `KraftApp.Builder.windowCtrl(...)`.
 */
interface WindowController {

    companion object {
        val key = TypedKey<WindowController>("WindowController")

        /** The browser-backed controller, created on first use; its listeners stay for the page's lifetime. */
        val default: WindowController by lazy { BrowserWindowController() }

        /** Current window inner size as a [Vector2D]. */
        fun windowSize(): Vector2D = Vector2D(window.innerWidth.toDouble(), window.innerHeight.toDouble())
    }

    /** Stream that emits true when the document has focus, false otherwise. */
    val hasFocus: Stream<Boolean>

    /** Stream that emits true while the page is visible, false while its tab is hidden or minimised. */
    val isVisible: Stream<Boolean>

    /**
     * Registers a [callback] to be invoked on window resize events.
     *
     * Prefer `lifecycle { onWindowResize { ... } }` on a component — that variant
     * auto-unsubscribes on unmount.
     *
     * @return an [Unsubscribe] function to remove the callback
     */
    fun onWindowResize(callback: (Vector2D) -> Unit): Unsubscribe

    /** Registers a [callback] to be invoked when the window gains focus. */
    fun onWindowFocus(callback: () -> Unit): Unsubscribe

    /** Registers a [callback] to be invoked when the window loses focus. */
    fun onWindowBlur(callback: () -> Unit): Unsubscribe
}

/** Kraft before ULTRA-00268 created one controller per app; the state is the browser's, so they share one now. */
@Deprecated("One controller serves the page", ReplaceWith("WindowController.default"))
@Suppress("FunctionName")
fun WindowController(): WindowController = WindowController.default

/** `document.visibilityState` is missing from Kotlin's `Document` binding. */
private fun isPageVisible(): Boolean = document.asDynamic().visibilityState != "hidden"

/** The [WindowController] backed by the browser's window and document events. */
private class BrowserWindowController : WindowController {

    private val hasFocusSource = StreamSource(document.hasFocus())

    override val hasFocus: Stream<Boolean> = hasFocusSource.readonly

    private val isVisibleSource = StreamSource(isPageVisible())

    override val isVisible: Stream<Boolean> = isVisibleSource.readonly

    private val resizeListeners = mutableListOf<(Vector2D) -> Unit>()
    private val focusListeners = mutableListOf<() -> Unit>()
    private val blurListeners = mutableListOf<() -> Unit>()

    init {
        val onVisibilityChange: (dynamic) -> Unit = {
            hasFocusSource(document.hasFocus())
            isVisibleSource(isPageVisible())
        }

        document.addEventListener("visibilitychange", onVisibilityChange)
        window.addEventListener("focus", ::onWindowFocusEvent)
        window.addEventListener("blur", ::onWindowBlurEvent)
        window.addEventListener("resize", ::onWindowResizeEvent)
    }

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

    @Suppress("UNUSED_PARAMETER")
    private fun onWindowResizeEvent(evt: Event) {
        val size = WindowController.windowSize()
        resizeListeners.toList().forEach { it(size) }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onWindowFocusEvent(evt: Event) {
        hasFocusSource(document.hasFocus())
        focusListeners.toList().forEach { it() }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onWindowBlurEvent(evt: Event) {
        hasFocusSource(document.hasFocus())
        blurListeners.toList().forEach { it() }
    }
}
