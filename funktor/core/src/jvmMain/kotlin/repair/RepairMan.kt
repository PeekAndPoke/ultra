package de.peekandpoke.funktor.core.repair

import de.peekandpoke.ultra.log.Log

class RepairMan(
    private val repairs: List<Repair>,
    private val log: Log,
) {
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
