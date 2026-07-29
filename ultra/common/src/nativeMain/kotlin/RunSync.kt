package io.peekandpoke.ultra.common

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.ThreadLocal

/** Nesting depth of [RunSync] on the current thread. Zero means this thread holds no lock. */
@ThreadLocal
private object LockDepth {
    var value = 0
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object RunSync {
    /** One global lock for the whole process — [invoke]'s `lock` argument is not consulted. */
    private val spinLock = AtomicInt(0)

    /**
     * Ignores [lock] and guards [block] with the single global [spinLock].
     *
     * Re-entrant: a nested call on a thread that already holds the lock runs straight through
     * instead of spinning on a lock it owns. This matters because several callers invoke
     * user-supplied callbacks while holding it.
     *
     * Unlike the JVM it still serialises callers that pass different [lock] objects, so it is
     * correct but coarser.
     */
    actual operator fun <R> invoke(lock: Any, block: () -> R): R {
        if (LockDepth.value > 0) {
            LockDepth.value++

            try {
                return block()
            } finally {
                LockDepth.value--
            }
        }

        while (!spinLock.compareAndSet(0, 1)) {
            // spin
        }

        LockDepth.value = 1

        try {
            return block()
        } finally {
            LockDepth.value = 0
            spinLock.value = 0
        }
    }
}
