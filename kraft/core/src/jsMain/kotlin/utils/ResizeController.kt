package de.peekandpoke.kraft.utils

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.ext.ResizeObserver
import de.peekandpoke.kraft.ext.ResizeObserverEntry
import de.peekandpoke.ultra.streams.Unsubscribe

fun <T> Component<T>.resizeCtrl() = ResizeController(this)

fun <T> Component.LifeCycle<T>.onResize(callback: (Array<ResizeObserverEntry>) -> Unit): Unsubscribe {
    val controller = component.resizeCtrl()

    return controller.onResize(callback)
}

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

    fun onResize(callback: (Array<ResizeObserverEntry>) -> Unit): Unsubscribe {
        subscriptions += callback

        return {
            subscriptions.remove(callback)
        }
    }
}
