package de.peekandpoke.kraft.utils

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.kraft.streams.addons.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.html.FlowContent

/**
 * Creates a data loader for the given [load] function.
 */
fun <T, C> Component<C>.dataLoader(load: suspend () -> Flow<T>): DataLoader<T> = DataLoader(
    component = this,
    options = DataLoader.Options(
        load = load,
    )
)

/**
 * Creates a data loader for a fixed [value].
 */
fun <T, C> Component<C>.dataLoaderOf(value: T): DataLoader<T> = dataLoader {
    flowOf(value)
}

class DataLoader<T>(
    component: Component<*>,
    val options: Options<T>,
) {
    data class Options<T>(
        val load: suspend () -> Flow<T>,
    )

    class Render<T> {
        internal var loading: FlowContent.() -> Unit = {}
        internal var loaded: FlowContent.(data: T) -> Unit = {}
        internal var error: FlowContent.(error: Throwable) -> Unit = {}

        fun loading(block: FlowContent.() -> Unit) {
            loading = block
        }

        fun loaded(block: FlowContent.(data: T) -> Unit) {
            loaded = block
        }

        fun error(block: FlowContent.(error: Throwable) -> Unit) {
            error = block
        }
    }

    sealed class State<T> {
        class Loading<T> : State<T>()
        class Loaded<T>(val data: T) : State<T>()
        class Error<T>(val error: Throwable) : State<T>()
    }

    private var currentState: State<T> by component.value(State.Loading()) {
        stateStream(it)
    }

    private var stateStream: StreamSource<State<T>> = StreamSource(currentState)

    /** The current state of the loader */
    val state: Stream<State<T>> = stateStream.readonly

    /** The current value of the loader */
    val value: Stream<T?> = state.map { (it as? State.Loaded<T>)?.data }

    private var requestsCounter = 0
    private val jobs = mutableListOf<Job>()

    init {
        reload(0)
    }

    /** Returns true when the loader is in the [State.Loading] */
    fun isLoading(): Boolean = state() is State.Loading

    /** Returns true when the loader is NOT in the [State.Loading] */
    fun isNotLoading(): Boolean = !isLoading()

    /** Returns true when the loader is in the [State.Error] */
    fun isError(): Boolean = state() is State.Error

    /** Returns true when the loader is NOT in the [State.Error] */
    fun isNotError(): Boolean = !isError()

    /** Returns true when the loader is in the [State.Loaded] */
    fun isLoaded(): Boolean = state() is State.Loaded

    /** Returns true when the loader is NOT in the [State.Loaded] */
    fun isNotLoaded(): Boolean = !isLoaded()

    operator fun invoke(flow: FlowContent, block: Render<T>.() -> Unit) {

        val render = Render<T>().apply(block)

        when (val s = currentState) {
            is State.Loading -> render.loading(flow)
            is State.Loaded<T> -> render.loaded(flow, s.data)
            is State.Error -> render.error(flow, s.error)
        }
    }

    fun modifyValue(block: (T) -> T) {
        value()?.let {
            setLoaded(
                block(it)
            )
        }
    }

    fun setLoaded(data: T) {
        setState(State.Loaded(data))
    }

    fun setState(state: State<T>) {
        currentState = state
    }

    fun reload(debounceMs: Long = 200) {
        currentState = State.Loading()
        reloadSilently(debounceMs)
    }

    fun reloadSilently(debounceMs: Long = 200) {
        jobs.forEach {
            try {
                it.cancel()
            } finally {
            }
        }

        lateinit var currentJob: Job

        fun onComplete(result: T) {
            if (currentJob == jobs.last()) {
                handleFinished()
                currentState = State.Loaded(result)

                jobs.clear()
            }
        }

        currentJob = launch {
            if (requestsCounter > 0) {
                delay(debounceMs)
            } else {
                delay(5)
            }

            try {
                val result = options.load()
                    .catch {
                        handleFinished()
                        handleException(it)
                    }
                    .first()

                onComplete(result)

            } catch (e: Throwable) {
                handleFinished()
                handleException(e)
            }
        }

        jobs.add(currentJob)
    }

    private fun handleFinished() {
        requestsCounter += 1
    }

    private fun handleException(e: Throwable) {
        when (e) {
            is CancellationException -> {
                // Do nothing if the current flow was cancelled
            }

            else -> {
                // Go to error state for each other exception
                currentState = State.Error(e)
            }
        }
    }
}
