package de.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RunSync {
    actual operator fun <R> invoke(lock: Any, block: () -> R): R = synchronized(lock, block)
}
