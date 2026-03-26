package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.ext.ResizeObserver
import io.peekandpoke.kraft.ext.ResizeObserverEntry
import io.peekandpoke.ultra.streams.Unsubscribe

/** Creates a [ResizeController] for this component. */
fun <T> Component<T>.resizeCtrl() = ResizeController(this)

/** Subscribes to resize events on the component's DOM element via a [ResizeController]. */
fun <T> Component.LifeCycle<T>.onResize(callback: (Array<ResizeObserverEntry>) -> Unit): Unsubscribe {
    val controller = component.resizeCtrl()

    return controller.onResize(callback)
}

/**
 * Observes resize events on a component's DOM element using the browser ResizeObserver API.
 *
 * Automatically attaches the observer on mount and disconnects on unmount.
 */
class ResizeController(component: Component<*>) {

    private val subscriptions = mutableListOf<(Array<ResizeObserverEntry>) -> Unit>()

    private var observer: ResizeObserver? = null

    init {
        component.lifecycle {
            onMount {
                // console.log("Mounting ResizeController")
                observer = ResizeObserver { entries, _ ->
                    // console.log("Resize event", entries)
                    subscriptions.forEach { it(entries) }
                }
                component.dom?.let { observer?.observe(it) }
            }

            onUnmount {
                // console.log("Unmounting ResizeController")
                observer?.disconnect()
                observer = null
            }
        }
    }

    /**
     * Registers a [callback] to be invoked on resize events.
     *
     * @return an [Unsubscribe] function to remove the callback
     */
    fun onResize(callback: (Array<ResizeObserverEntry>) -> Unit): Unsubscribe {
        subscriptions += callback

        return {
            subscriptions.remove(callback)
        }
    }
}
