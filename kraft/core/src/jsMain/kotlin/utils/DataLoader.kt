package io.peekandpoke.kraft.utils

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.ops.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

/**
 * Asynchronous data loader that manages loading, loaded, and error states for a component.
 *
 * Automatically triggers the initial load on creation and supports debounced reloading.
 */
class DataLoader<T>(
    component: Component<*>,
    val options: Options<T>,
) {
    /** Configuration options for the data loader. */
    data class Options<T>(
        val load: suspend () -> Flow<T>,
    )

    /**
     * DSL builder for rendering the data loader's current state.
     *
     * Provides callbacks for each possible state: [loading], [loaded], and [error].
     */
    class Render<T> {
        internal var loading: FlowContent.() -> Unit = {}
        internal var loaded: FlowContent.(data: T) -> Unit = {}
        internal var error: FlowContent.(error: Throwable) -> Unit = {}

        /** Sets the render block for the loading state. */
        fun loading(block: FlowContent.() -> Unit) {
            loading = block
        }

        /** Sets the render block for the loaded state. */
        fun loaded(block: FlowContent.(data: T) -> Unit) {
            loaded = block
        }

        /** Sets the render block for the error state. */
        fun error(block: FlowContent.(error: Throwable) -> Unit) {
            error = block
        }
    }

    /** Represents the current state of the data loader. */
    sealed class State<T> {
        /** Data is currently being loaded. */
        class Loading<T> : State<T>()

        /** Data has been successfully loaded. */
        class Loaded<T>(val data: T) : State<T>()

        /** Loading failed with an [error]. */
        class Error<T>(val error: Throwable) : State<T>()
    }

    private var currentState: State<T> by component.value(State.Loading()) {
        stateStream(it)
    }

    private var stateStream: StreamSource<State<T>> = StreamSource(currentState)

    /** The current state of the loader as a stream. */
    val state: Stream<State<T>> = stateStream.readonly

    /** The current loaded value, or null if not yet loaded or in error state. */
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

    /**
     * Renders the current state into the given [flow] content using the [Render] DSL.
     */
    operator fun invoke(flow: FlowContent, block: Render<T>.() -> Unit) {

        val render = Render<T>().apply(block)

        when (val s = currentState) {
            is State.Loading -> render.loading(flow)
            is State.Loaded<T> -> render.loaded(flow, s.data)
            is State.Error -> render.error(flow, s.error)
        }
    }

    /** Modifies the currently loaded value using the given [block] transformation. Does nothing if not loaded. */
    fun modifyValue(block: (T) -> T) {
        value()?.let {
            setLoaded(
                block(it)
            )
        }
    }

    /** Transitions the loader to the [State.Loaded] state with the given [data]. */
    fun setLoaded(data: T) {
        setState(State.Loaded(data))
    }

    /** Directly sets the loader to the given [state]. */
    fun setState(state: State<T>) {
        currentState = state
    }

    /**
     * Reloads the data, first transitioning to the [State.Loading] state.
     *
     * @param debounceMs debounce delay in milliseconds to avoid rapid successive requests
     */
    fun reload(debounceMs: Long = 200) {
        currentState = State.Loading()
        reloadSilently(debounceMs)
    }

    /**
     * Reloads the data without transitioning to the loading state, keeping the current value visible.
     *
     * @param debounceMs debounce delay in milliseconds to avoid rapid successive requests
     */
    fun reloadSilently(debounceMs: Long = 200) {
        jobs.forEach {
            try {
                it.cancel()
            } catch (_: Throwable) {
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
                val result = options.load().first()

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
