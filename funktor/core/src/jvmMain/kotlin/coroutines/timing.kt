package coroutines

import de.peekandpoke.funktor.core.model.CpuProfile
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
): CpuProfile.WithValue<T> {
    val timer = TimingInterceptor()

    val result = withContext(timer + CoroutineName(name)) {
        block()
    }

    // Let the timer complete its work. This is necessary because the timer is a child coroutine,
    // and we need to wait for it to complete before we can get the timing.
    // Otherwise, some child timing will be incompletely recorded
    delay(1.microseconds)

    return CpuProfile.WithValue(
        value = result,
        profile = timer.getCpuProfile().children.first(),
    )
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class TimingInterceptor : CopyableThreadContextElement<TimingInterceptor.State>,
    AbstractCoroutineContextElement(TimingInterceptor) {

    companion object Key : CoroutineContext.Key<TimingInterceptor>

    class State(val startTime: Long)

    private val start: Long = System.nanoTime()
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

    fun getCpuProfile(): CpuProfile {
        val totalMs = (System.nanoTime() - start) / 1e6
        val cpuMs = activeNs.get() / 1e6

        // get coroutine name
        val coroutineName = capturedContext?.get(CoroutineName)?.name ?: "unknown"

        // get the dispatcher name
        val dispatcherName = capturedContext?.get(CoroutineDispatcher)?.toString()

        val timing = CpuProfile(
            names = listOfNotNull(coroutineName, dispatcherName),
            timeMs = totalMs,
            cpuMs = cpuMs,
            children = children.map { it.getCpuProfile() }
        )

        return timing
    }
}
