package de.peekandpoke.funktor.cluster.locks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

abstract class GlobalLocksProviderBase : GlobalLocksProvider {

    @Throws(LocksException.Timeout::class, LocksException.Execution::class)
    abstract suspend fun <T> FlowCollector<T>.doAcquire(key: String, timeout: Duration, handler: suspend () -> T)

    @Throws(LocksException.Timeout::class, LocksException.Execution::class)
    final override fun <T> acquire(key: String, timeout: Duration, handler: suspend () -> T): Flow<T> {
        return flow {
            doAcquire(key, timeout, handler)
        }
    }
}
