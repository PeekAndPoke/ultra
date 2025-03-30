package de.peekandpoke.kraft.utils

import de.peekandpoke.kraft.streams.StreamSource

class SimpleAsyncQueue {
    data class State(
        val queueSize: Int = 0,
    )

    private val source = StreamSource(State())
    val state = source.readonly

    private val jobs = mutableListOf<suspend () -> Unit>()

    private var running: Boolean = false

    fun isEmpty() = jobs.isEmpty()

    fun size() = jobs.size

    fun add(job: suspend () -> Unit) {
        jobs.add(job)
        notify()
        runNext()
    }

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
