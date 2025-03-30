package de.peekandpoke.ktorfx.cluster.locks

import kotlin.time.Duration

sealed class LocksException(message: String?, cause: Throwable? = null) : Throwable(message = message, cause = cause) {

    companion object {
        private fun getInvoker() = Exception("Lock initiated by")
    }

    class Timeout(message: String, cause: Throwable) : LocksException(message = message, cause = cause) {
        constructor(key: String, duration: Duration, cause: Throwable = getInvoker()) : this(
            message = "Locking '$key' timed out after ${duration.toIsoString()}",
            cause = cause,
        )
    }

    class Execution(cause: Throwable? = null) : LocksException(message = cause?.message, cause)
}


