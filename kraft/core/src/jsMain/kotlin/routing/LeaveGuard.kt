package io.peekandpoke.kraft.routing

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.window
import org.w3c.dom.events.Event

/** What a `guardLeaving` block answers when the router is about to leave the page (ULTRA-00266, from Klang). */
sealed interface LeaveResult {
    /** Leave. */
    data object Proceed : LeaveResult

    /** Stay: the navigation is dropped, and a Back, a Forward or a typed hash (hash routing) is undone. */
    data object Stop : LeaveResult

    /** Stay for now and ask, e.g. with a modal: [confirm] answering true goes on to the target, false stays. */
    class Ask(val confirm: suspend () -> Boolean) : LeaveResult
}

/**
 * Asked before the router leaves the current page, with the target uri; `null` when the page itself unloads. Leaving
 * means another path (before `?` and `#`), which is what RouterComponent remounts the page for. A change of query or
 * fragment alone never asks, so a `urlParam` filter does not.
 */
internal fun interface LeaveGuard {
    fun onLeave(target: String?): LeaveResult
}

/**
 * Guards leaving the page this component is on, for as long as it is mounted: the router asks [block] before each
 * navigation to another path (`navToUri`, a link it intercepts, `replaceUri`, `navBack`, Back and Forward, a typed
 * hash under hash routing). Use it inside the routed page: it binds to the page the router is on when it mounts, and a
 * component mounted before the router has a route (an app shell) is never asked.
 *
 * ```
 * guardLeaving { if (draft.isDirty) LeaveResult.Ask { modals.confirmLeave() } else LeaveResult.Proceed }
 * ```
 *
 * The block is asked at the moment of navigation, so it decides by the state then. A page load is not the router's:
 * reload, tab close, a typed address or bookmark under path routing, an external link, a Back out of the app. With
 * [beforeUnload] those show the browser's own prompt while the block answers anything but [LeaveResult.Proceed] (an
 * [LeaveResult.Ask] cannot run its modal there), unless the page load starts in, or within 3 s after,
 * `router.withoutLeaveGuards { }`.
 * Several guards are asked in mount order; the first that does not proceed decides.
 */
fun Component<*>.guardLeaving(beforeUnload: Boolean = false, block: (target: String?) -> LeaveResult) {
    val guard = LeaveGuard(block)
    var unsubscribe: Unsubscribe? = null

    val onBeforeUnload: (Event) -> Unit = { event ->
        // A page load inside withoutLeaveGuards (a logout through the server) leaves without the prompt
        if (!router.passingLeaveGuards && block(null) != LeaveResult.Proceed) {
            event.preventDefault()
            // Older browsers only show the prompt with a returnValue
            event.asDynamic().returnValue = ""
        }
    }

    lifecycle {
        onMount {
            unsubscribe = router.addLeaveGuard(guard)

            if (beforeUnload) window.addEventListener("beforeunload", onBeforeUnload)
        }

        onUnmount {
            unsubscribe?.invoke()
            unsubscribe = null

            if (beforeUnload) window.removeEventListener("beforeunload", onBeforeUnload)
        }
    }
}
