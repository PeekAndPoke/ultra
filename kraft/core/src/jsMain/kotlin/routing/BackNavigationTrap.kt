package io.peekandpoke.kraft.routing

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.utils.SimpleAsyncQueue
import kotlinx.browser.window
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event

/**
 * Intercepts browser back navigation to give the component a chance to cancel or handle it.
 *
 * Pushes a dummy history state and listens for popstate events. The [block] callback
 * decides whether to [TrapResult.Stop] the navigation or [TrapResult.Continue].
 *
 * A popstate that lands on the trap's own entry is ignored: it comes from a trap or route stacked above going back.
 */
class BackNavigationTrap(
    private val component: Component<*>,
    private val block: () -> TrapResult,
) {
    companion object {
        private var counter = 0

        /** Extension to set up a back-navigation trap on a component. */
        fun Component<*>.trapBackNavigation(block: () -> TrapResult) = BackNavigationTrap(
            component = this,
            block = block,
        )
    }

    /** Result returned by the trap callback to control navigation behavior. */
    enum class TrapResult {
        /** Prevents the back navigation. */
        Stop,

        /** Allows the back navigation to proceed. */
        Continue,
    }

    private val queue = SimpleAsyncQueue()

    private var isActive: Boolean = false

    private val data = "--navigation-trap-${counter++}--"

    /** The address of the entry the trap pushed. */
    private var pushedHref: String? = null

    init {
        component.lifecycle {
            onMount {
                activate()
            }

            onUnmount {
                deactivate()
            }
        }
    }

    private val onPopState: (PopStateEvent) -> Unit = handler@{ event: PopStateEvent ->

        // Landed on our own entry: a trap stacked above us went back (e.g. a modal on top closed). Not a back past us.
        if (event.state == data) return@handler

        when (block()) {
            // The user navigated away: deactivate immediately without going back
            TrapResult.Continue -> deactivateInternal { }
            // If not we have to push the state again
            TrapResult.Stop -> pushState()
        }
    }

    /** Activates the trap by pushing a history state and listening for popstate events. */
    fun activate() {
        if (!isActive) {
            isActive = true

            pushState()

            @Suppress("UNCHECKED_CAST")
            window.addEventListener("popstate", onPopState as (Event) -> Unit)
        }
    }

    /**
     * Deactivates the trap and goes back over its entry, while that entry still has the address the trap pushed; after
     * a change of address (a replaceUri while the trap was up) it stays and only takes its marker off the entry.
     */
    fun deactivate() {
        deactivateInternal {
            goBack()
        }
    }

    private fun deactivateInternal(block: () -> Unit) {
        if (isActive) {
            isActive = false

            @Suppress("UNCHECKED_CAST")
            window.removeEventListener("popstate", onPopState as (Event) -> Unit)

            block()
        }
    }

    private fun pushState() {
        if (window.history.state != data) {
            pushedHref = window.location.href

            @Suppress("UnsafeCastFromDynamic")
            window.history.pushState(
                data = data,
                title = undefined.asDynamic(),
                url = window.location.href,
            )
        }
    }

    private fun goBack() {
        queue.add { doGoBack() }
    }

    private fun doGoBack() {
        // Still on the trap's own entry?
        if (window.history.state != data) return

        if (window.location.href == pushedHref) {
            window.history.back()
        } else {
            // The address changed while the trap was up (a replaceUri, e.g. a urlParam inside a modal): going back
            // would land on the entry before that change and undo it. Stay, and only take the trap's marker off.
            window.history.replaceState(null, "", window.location.href)
        }
    }
}
