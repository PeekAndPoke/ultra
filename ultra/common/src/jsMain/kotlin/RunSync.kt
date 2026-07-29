package io.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RunSync {
    /**
     * Runs [block] directly and ignores [lock].
     *
     * A non-suspending block cannot be interleaved on a single JS event loop, but two workers
     * sharing a `SharedArrayBuffer`-backed object get no mutual exclusion at all.
     */
    actual operator fun <R> invoke(lock: Any, block: () -> R): R = block()
}
