@file:Suppress("detekt:all")

package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.*
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.ultra.common.recursion.recurse
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.domain.DebugInfo

class KontainerCollector(
    private val kontainer: Kontainer,
    private val blueprint: KontainerBlueprint,
) : InsightsCollector {

    /** VUE-REF: `reference/collectors/KontainerCollector.kt` */
    data class Data(
        val numOld: Int,
        val numTotal: Int,
        val info: DebugInfo,
    ) : InsightsCollectorData {
        override val key = KEY

        companion object {
            const val KEY = "kontainer"
        }
    }


    override fun finish(call: ApplicationCall): Data {

        return try {
            val numTotal = blueprint.tracker.getNumAlive()
            // TODO: make the threshold configurable
            val numOld = blueprint.tracker.getNumAlive(15)

            Data(
                numOld = numOld,
                numTotal = numTotal,
                info = kontainer.tools.getDebugInfo(),
            )
        } catch (e: Throwable) {
            println("[ERROR] Could not get kontainer DebugInfo!\n${e.stackTraceToString()}")

            Data(
                numOld = 0,
                numTotal = 0,
                info = DebugInfo(services = emptyList())
            )
        }
    }
}
