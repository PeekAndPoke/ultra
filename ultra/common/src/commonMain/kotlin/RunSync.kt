package io.peekandpoke.ultra.common

/**
 * Multiplatform synchronization primitive.
 *
 * On JVM, delegates to `synchronized`; on JS, executes the block directly (single-threaded).
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object RunSync {
    /**
     * Executes the given [block] while holding a lock on [lock], returning its result.
     */
    operator fun <R> invoke(lock: Any, block: () -> R): R
}
