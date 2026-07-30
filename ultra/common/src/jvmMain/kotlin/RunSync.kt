package io.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RunSync {
    /** Uses [lock]'s monitor, so distinct locks are independent and nesting is re-entrant. */
    actual operator fun <R> invoke(lock: Any, block: () -> R): R = synchronized(lock, block)
}
