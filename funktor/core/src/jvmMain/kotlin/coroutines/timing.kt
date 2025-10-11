package coroutines

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

suspend fun <T> measureCoroutine(block: suspend () -> T): TimingInterceptor.TimedResult<T> {
    val timer = TimingInterceptor()

    val result = withContext(timer) {
        block()
    }


    return TimingInterceptor.TimedResult(
        value = result,
        timing = timer.getTiming()
    )
}

class TimingInterceptor : ThreadContextElement<TimingInterceptor.State>,
    AbstractCoroutineContextElement(TimingInterceptor) {

    companion object Key : CoroutineContext.Key<TimingInterceptor>

    data class TimedResult<T>(
        val value: T,
        val timing: Timing,
    )

    data class Timing(
        val totalMs: Double,
        val cpuMs: Double,
    ) {
        val idleMs = (totalMs - cpuMs).coerceAtLeast(0.0)

        val cpuPct: Double get() = cpuMs / totalMs
        val idlePct: Double get() = idleMs / totalMs
    }

    class State(val startTime: Long)

    private val start = System.nanoTime()
    private val activeNs = AtomicLong(0)

    override fun updateThreadContext(context: CoroutineContext): State {
        // Called when coroutine switches to this thread
        return State(System.nanoTime())
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: State) {
        // Called when coroutine leaves this thread
        val elapsed = System.nanoTime() - oldState.startTime
        activeNs.addAndGet(elapsed)
    }

    fun getTiming(): Timing {
        val total = (System.nanoTime() - start) / 1e6
        val active = activeNs.get() / 1e6

        return Timing(totalMs = total, cpuMs = active)
    }
}
