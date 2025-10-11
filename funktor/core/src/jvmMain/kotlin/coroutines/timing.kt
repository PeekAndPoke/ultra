package coroutines

import kotlinx.coroutines.CopyableThreadContextElement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.microseconds

suspend fun <T> measureCoroutine(
    name: String = "ROOT",
    block: suspend CoroutineScope.() -> T,
): TimingInterceptor.TimedResult<T> {
    val timer = TimingInterceptor()

    val result = withContext(timer + CoroutineName(name)) {
        block()
    }

    // Let the timer complete its work. This is necessary because the timer is a child coroutine,
    // and we need to wait for it to complete before we can get the timing.
    // Otherwise, some child timing will be incompletely recorded
    delay(1.microseconds)

    return TimingInterceptor.TimedResult(
        value = result,
        timing = timer.getTiming().children.first(),
    )
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class TimingInterceptor : CopyableThreadContextElement<TimingInterceptor.State>,
    AbstractCoroutineContextElement(TimingInterceptor) {

    companion object Key : CoroutineContext.Key<TimingInterceptor>

    data class TimedResult<T>(
        val value: T,
        val timing: Timing,
    )

    data class Timing(
        val coroutineName: String,
        val dispatcherName: String?,
        val timeMs: Double,
        val cpuMs: Double,
        val children: List<Timing> = emptyList(),
    ) {
        val cpuUsage: Double = cpuMs / timeMs
        val cpuUsagePct: Double = cpuUsage * 100

        val totalCpuMs: Double by lazy {
            cpuMs + children.sumOf { it.totalCpuMs }
        }

        val totalCpuUsage: Double by lazy {
            cpuUsage + children.sumOf { it.totalCpuUsage }
        }

        val totalCpuUsagePct: Double by lazy { totalCpuUsage * 100 }

        fun plot(indent: String = ""): String = buildString {
            append("$indent$coroutineName")
            dispatcherName?.let { append(" ($it)") }
            appendLine(": ${"%.2f".format(timeMs)} ms, ${"%.2f".format(cpuUsagePct)} % CPU")
            children.forEach {
                append(it.plot("$indent  "))
            }
        }
    }

    class State(val startTime: Long)

    private val start = System.nanoTime()
    private val activeNs = AtomicLong(0)
    private var capturedContext: CoroutineContext? = null

    private val children = mutableListOf<TimingInterceptor>()

    override fun updateThreadContext(context: CoroutineContext): State {
        // Called when coroutine switches to this thread
        capturedContext = capturedContext ?: context

        return State(System.nanoTime())
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: State) {
        // Called when coroutine leaves this thread
        val elapsed = System.nanoTime() - oldState.startTime
        activeNs.addAndGet(elapsed)
    }

    override fun copyForChild(): TimingInterceptor {
        return TimingInterceptor().also {
            children.add(it)
        }
    }

    override fun mergeForChild(overwritingElement: CoroutineContext.Element): TimingInterceptor {
        return this
    }

    fun getTiming(): Timing {
        val totalMs = (System.nanoTime() - start) / 1e6
        val cpuMs = activeNs.get() / 1e6

        // get coroutine name
        val coroutineName = capturedContext?.get(CoroutineName)?.name ?: "unknown"

        // get the dispatcher name
        val dispatcherName = capturedContext?.get(CoroutineDispatcher)?.toString()

        val timing = Timing(
            coroutineName = coroutineName,
            dispatcherName = dispatcherName,
            timeMs = totalMs,
            cpuMs = cpuMs,
            children = children.map { it.getTiming() }
        )

        return timing
    }
}
