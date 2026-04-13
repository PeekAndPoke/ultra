package io.peekandpoke.funktor.core.repair

import io.peekandpoke.ultra.log.Log

/** Runs all registered [Repair] actions, logging progress and errors. */
class RepairMan(
    val repairs: List<Repair>,
    private val log: Log,
) {
    /** A single repair action to be run by [RepairMan]. */
    interface Repair {
        fun run()
    }

    fun run() {
        repairs.forEach {
            try {
                log.info("Running repair '${it::class}'")
                it.run()
                log.info("Finished repair '${it::class}'")
            } catch (t: Throwable) {
                log.error("Error running repair '${it::class}'\n\n${t.stackTraceToString()}")
            }
        }
    }
}
