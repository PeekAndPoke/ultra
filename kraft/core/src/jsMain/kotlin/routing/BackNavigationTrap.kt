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

    /** Deactivates the trap and navigates back to remove the dummy history entry. */
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
        val shouldGoBack = window.history.state == data
//        console.log(window.history.state, data, window.location.href, shouldGoBack)

        // Are we still on the same navigation state?
        if (shouldGoBack) {
            window.history.back()
            console.log("going back")

            // Wait for the browser to catch up, as the history.back() is async itself
//            delay(100)
//            console.log(window.history.state, data, window.location.href, shouldGoBack)
        }
    }
}
