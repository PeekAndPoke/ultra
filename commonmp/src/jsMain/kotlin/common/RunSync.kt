package de.peekandpoke.ultra.common

actual object RunSync {
    actual operator fun <R> invoke(lock: Any, block: () -> R): R = block()
}
