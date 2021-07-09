package de.peekandpoke.ultra.common

expect object RunSync {
    operator fun <R> invoke(lock: Any, block: () -> R): R
}
