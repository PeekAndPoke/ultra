package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.ultra.common.TypedKey
import kotlinx.coroutines.Deferred

/** Creates a new [DoubleClickProtection] instance bound to this component. */
fun <T> Component<T>.doubleClickProtection() = DoubleClickProtection(this)

/** Retrieves or creates a shared [DoubleClickProtection] instance from the component's attributes. */
val <T> Component<T>.noDblClick: DoubleClickProtection
    get() {
        return attributes.getOrPut(DoubleClickProtection.key) { DoubleClickProtection(this) }
    }

/**
 * Prevents concurrent execution of async actions, guarding against accidental double-clicks.
 *
 * While an action is running, subsequent invocations return null instead of starting a second execution.
 */
class DoubleClickProtection(component: Component<*>) {

    companion object {
        val key = TypedKey<DoubleClickProtection>("DoubleClickProtection")
    }

    private var counter: Int by component.value(0)

    /** True when no action is currently running. */
    val canRun: Boolean get() = counter == 0

    /** True when an action is currently running. */
    val cannotRun: Boolean get() = !canRun

    /** Runs [block] asynchronously if no action is in progress, otherwise returns null. */
    operator fun <T> invoke(block: suspend () -> T): Deferred<T>? {
        return runAsync(block)
    }

    /**
     * Runs [block] asynchronously if no action is in progress, otherwise returns null.
     *
     * The protection counter is decremented when the block completes or throws.
     */
    fun <T> runAsync(block: suspend () -> T): Deferred<T>? {
        if (cannotRun) {
            return null
        }

        counter++

        return async {
            try {
                block()
            } catch (e: Throwable) {
                throw e
            } finally {
                counter--
            }
        }
    }

    /**
     * Runs [block] as a suspend function if no action is in progress, otherwise returns null.
     *
     * The protection counter is decremented when the block completes or throws.
     */
    suspend fun <R> runBlocking(block: suspend () -> R): R? {
        if (cannotRun) {
            return null
        }

        counter++

        return try {
            block()
        } catch (e: Throwable) {
            throw e
        } finally {
            counter--
        }
    }
}
