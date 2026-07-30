package io.peekandpoke.ultra.common

/**
 * Multiplatform synchronization primitive.
 *
 * On JVM, delegates to `synchronized`; on JS, executes the block directly (single-threaded);
 * on native, a single process-wide spin lock serialises every call.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object RunSync {
    /**
     * Executes the given [block] while holding a lock on [lock], returning its result.
     *
     * Only the JVM honours [lock] as a distinct monitor and allows re-entrant nesting. Do not nest
     * calls and do not rely on independent locks running concurrently — neither holds on native.
     */
    operator fun <R> invoke(lock: Any, block: () -> R): R
}
