package io.peekandpoke.kraft.routing

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.window
import org.w3c.dom.events.Event

/** What a [LeaveGuard] answers when the router is about to leave the current route (ULTRA-00266, from Klang). */
sealed interface LeaveResult {
    /** Leave. */
    data object Proceed : LeaveResult

    /** Stay: the navigation is dropped, and a Back or a typed address is undone. */
    data object Stop : LeaveResult

    /** Stay for now and ask, e.g. with a modal: [confirm] answering true goes on to the target, false stays. */
    class Ask(val confirm: suspend () -> Boolean) : LeaveResult
}

/**
 * Asked before the router leaves the current route, with the target uri; `null` when the page itself unloads (reload,
 * tab close). Leaving means another matched route: another route pattern or other route params. A change of query or
 * fragment alone never asks, so a `urlParam` filter does not.
 */
fun interface LeaveGuard {
    fun onLeave(target: String?): LeaveResult
}

/**
 * Guards leaving the page this component is on, for as long as it is mounted: the router asks [block] before each
 * navigation away (`navToUri`, a link it intercepts, `replaceUri`, Back and Forward, a typed hash).
 *
 * ```
 * guardLeaving { if (draft.isDirty) LeaveResult.Ask { modals.confirmLeave() } else LeaveResult.Proceed }
 * ```
 *
 * The block is asked at the moment of navigation, so it decides by the state then. With [beforeUnload] a reload or a
 * tab close shows the browser's own prompt while the block answers anything but [LeaveResult.Proceed] (an
 * [LeaveResult.Ask] cannot run its modal there). Several guards are asked in mount order; the first that does not
 * proceed decides.
 */
fun Component<*>.guardLeaving(beforeUnload: Boolean = false, block: (target: String?) -> LeaveResult) {
    val guard = LeaveGuard(block)
    var unsubscribe: Unsubscribe? = null

    val onBeforeUnload: (Event) -> Unit = { event ->
        if (block(null) != LeaveResult.Proceed) {
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
