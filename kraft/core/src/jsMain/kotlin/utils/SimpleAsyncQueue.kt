package io.peekandpoke.kraft.utils

import io.peekandpoke.ultra.streams.StreamSource

/**
 * A simple FIFO queue that executes suspend jobs one at a time in order.
 *
 * Jobs are run sequentially; each job must complete before the next one starts.
 */
class SimpleAsyncQueue {
    /** Observable state of the queue. */
    data class State(
        /** Number of jobs currently in the queue. */
        val queueSize: Int = 0,
    )

    private val source = StreamSource(State())

    /** Stream of the current queue [State]. */
    val state = source.readonly

    private val jobs = mutableListOf<suspend () -> Unit>()

    private var running: Boolean = false

    /** Returns true when the queue has no pending jobs. */
    fun isEmpty() = jobs.isEmpty()

    /** Returns the number of pending jobs. */
    fun size() = jobs.size

    /** Adds a [job] to the queue and starts processing if idle. */
    fun add(job: suspend () -> Unit) {
        jobs.add(job)
        notify()
        runNext()
    }

    /** Removes all pending jobs from the queue. */
    fun clear() {
        jobs.clear()
        notify()
    }

    private fun notify() {
        try {
            source(State(queueSize = jobs.size))
        } finally {
        }
    }

    private fun runNext() {

        if (jobs.isEmpty()) {
            return
        }

        if (!running) {
            running = true

            launch {
                val nextJob = jobs.firstOrNull()

                nextJob?.let {
                    try {
                        nextJob()
                    } catch (e: Throwable) {
                        console.error("Job failed", e)
                    }

                    // Remove the job
                    jobs.remove(nextJob)
                }

                // Notify
                notify()

                running = false

                runNext()
            }
        }
    }
}
