package io.peekandpoke.funktor.cluster.locks

import kotlin.time.Duration

/** Base exception for all lock-related errors. */
sealed class LocksException(message: String?, cause: Throwable? = null) : Throwable(message = message, cause = cause) {

    companion object {
        private fun getInvoker() = Exception("Lock initiated by")
    }

    /** Thrown when acquiring a lock exceeds the configured timeout. */
    class Timeout(message: String, cause: Throwable) : LocksException(message = message, cause = cause) {
        constructor(key: String, duration: Duration, cause: Throwable = getInvoker()) : this(
            message = "Locking '$key' timed out after ${duration.toIsoString()}",
            cause = cause,
        )
    }

    /** Wraps an exception thrown inside a locked handler. */
    class Execution(cause: Throwable? = null) : LocksException(message = cause?.message, cause)
}
