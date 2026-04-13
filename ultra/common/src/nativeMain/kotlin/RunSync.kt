package io.peekandpoke.ultra.common

import kotlin.concurrent.AtomicInt

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RunSync {
    private val spinLock = AtomicInt(0)

    actual operator fun <R> invoke(lock: Any, block: () -> R): R {
        while (!spinLock.compareAndSet(0, 1)) {
            // spin
        }
        try {
            return block()
        } finally {
            spinLock.value = 0
        }
    }
}
