package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.Display
import kotlinx.css.display
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.events.Event
import kotlin.js.Promise

/** A held screen wake lock of the Screen Wake Lock API; the browser releases it by itself when the page is hidden. */
external interface WakeLockSentinel {
    fun addEventListener(type: String, callback: () -> Unit)
    fun release(): Promise<Unit>
}

/**
 * Keeps the screen from dimming while someone holds it (ULTRA-00275): a counter over one screen wake lock. The first
 * [acquire] requests the lock, the last release lets it go, so several holders (several [WakeLockComponent]s) share
 * it. The browser drops the lock when the page is hidden; it is requested again when the page is visible and someone
 * still holds it. Without the API (an old browser, an insecure context) nothing happens.
 *
 * One lock serves the page: [default], registered in every app; components reach it as [wakeLockCtrl]. Another API
 * (a test's fake) goes into the constructor, and into the app with `kraftApp { wakeLockCtrl(...) }`.
 */
class WakeLockController(
    /** Asks for a screen lock; null when the API is missing. */
    private val requestLock: () -> Promise<WakeLockSentinel>?,
    /** Whether the page is visible: a lock is only granted then. */
    private val isPageVisible: () -> Boolean,
) {
    companion object {
        val key = TypedKey<WakeLockController>("WakeLockController")

        /** The page's controller, on the browser's API, created on first use. */
        val default: WakeLockController by lazy {
            WakeLockController(
                requestLock = {
                    val api = window.navigator.asDynamic().wakeLock
                    if (api == null || api == undefined) {
                        null
                    } else {
                        api.request("screen").unsafeCast<Promise<WakeLockSentinel>>()
                    }
                },
                isPageVisible = { document.asDynamic().visibilityState != "hidden" },
            )
        }

        /** Requests after a failure, one a second apart, while held and visible. */
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000
    }

    private var holders = 0

    private var sentinel: WakeLockSentinel? = null

    private var requesting = false

    private var retries = 0

    private val active = StreamSource(false)

    /** True while the browser holds the lock for this page. */
    val isActive: Stream<Boolean> = active.readonly

    /** How many hold the lock now. */
    val holderCount: Int get() = holders

    // A visible page that is held asks for the lock again (the browser dropped it when the page was hidden). Listened
    // to only while someone holds the lock: a controller nobody holds (a test's fake) leaves nothing on the document.
    private val onVisibility: (Event) -> Unit = {
        if (isPageVisible()) {
            retries = 0
            request()
        }
    }

    /** Holds the lock until the returned release is called; calling it again does nothing. */
    fun acquire(): () -> Unit {
        if (holders++ == 0) document.addEventListener("visibilitychange", onVisibility)
        retries = 0
        request()

        var released = false

        return {
            if (!released) {
                released = true
                holders--

                if (holders == 0) {
                    document.removeEventListener("visibilitychange", onVisibility)
                    letGo()
                }
            }
        }
    }

    private fun request() {
        if (holders == 0 || sentinel != null || requesting || !isPageVisible()) return

        val pending = try {
            requestLock()
        } catch (e: dynamic) {
            console.warn("[WakeLock] request failed", e)
            null
        } ?: return

        requesting = true

        pending.then(
            onFulfilled = { lock ->
                requesting = false
                granted(lock)
            },
            onRejected = { error ->
                requesting = false
                failed(error)
            },
        )
    }

    private fun granted(granted: WakeLockSentinel) {
        // Everyone let go while the request was pending
        if (holders == 0) {
            releaseQuietly(granted)
            return
        }

        sentinel = granted
        active(true)

        granted.addEventListener("release") {
            // The browser took it (the page was hidden); ours, released by letGo, is no longer the sentinel
            if (sentinel === granted) {
                sentinel = null
                active(false)
                request()
            }
        }
    }

    private fun failed(error: Throwable) {
        console.warn("[WakeLock] not granted", error)

        if (holders > 0 && retries < MAX_RETRIES) {
            retries++
            setTimeout(RETRY_DELAY_MS) { request() }
        }
    }

    private fun letGo() {
        val held = sentinel ?: return

        sentinel = null
        active(false)
        releaseQuietly(held)
    }

    private fun releaseQuietly(lock: WakeLockSentinel) {
        try {
            lock.release().catch { error -> console.warn("[WakeLock] release failed", error) }
        } catch (e: dynamic) {
            console.warn("[WakeLock] release failed", e)
        }
    }
}

/** Retrieves the app's [WakeLockController] from the component's attribute hierarchy. */
val Component<*>.wakeLockCtrl: WakeLockController get() = getAttributeRecursive(WakeLockController.key)

/**
 * Keeps the screen awake while it is mounted (ULTRA-00275): it holds the app's [WakeLockController] from mount to
 * unmount, so several can be on the page at once and the lock goes when the last one does. It renders nothing visible.
 */
@Suppress("FunctionName")
fun Tag.WakeLockComponent() = comp { WakeLockComponent(it) }

/** See [WakeLockComponent]. */
class WakeLockComponent(ctx: NoProps) : PureComponent(ctx) {

    private var release: (() -> Unit)? = null

    init {
        lifecycle {
            onMount { release = wakeLockCtrl.acquire() }

            onUnmount {
                release?.invoke()
                release = null
            }
        }
    }

    override fun VDom.render() {
        div(classes = "kraft-wake-lock") {
            css { display = Display.none }
        }
    }
}
