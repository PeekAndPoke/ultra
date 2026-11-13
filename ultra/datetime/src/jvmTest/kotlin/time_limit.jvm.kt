package io.peekandpoke.ultra.datetime

import kotlin.time.Duration

actual fun <T> withinTimeLimit(limit: Duration, block: () -> T): T {
    var outcome: Result<T>? = null

    // A daemon thread: a runaway block cannot be stopped, but it must not keep the test JVM alive either.
    val worker = Thread { outcome = runCatching(block) }.apply {
        isDaemon = true
        start()
    }

    worker.join(limit.inWholeMilliseconds)

    if (worker.isAlive) {
        throw AssertionError("Did not finish within $limit")
    }

    return outcome!!.getOrThrow()
}
