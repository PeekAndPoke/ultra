package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.ultra.common.TypedKey
import kotlinx.coroutines.Deferred

fun <T> Component<T>.doubleClickProtection() = DoubleClickProtection(this)

val <T> Component<T>.noDblClick: DoubleClickProtection
    get() {
        return attributes.getOrPut(DoubleClickProtection.key) { DoubleClickProtection(this) }
    }

class DoubleClickProtection(component: Component<*>) {

    companion object {
        val key = TypedKey<DoubleClickProtection>("DoubleClickProtection")
    }

    private var counter: Int by component.value(0)

    val canRun: Boolean get() = counter == 0

    val cannotRun: Boolean get() = !canRun

    operator fun <T> invoke(block: suspend () -> T): Deferred<T>? {
        return runAsync(block)
    }

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
