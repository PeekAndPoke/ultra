package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.InsightsGraphs
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import java.time.Instant
import kotlin.reflect.KClass

/**
 * The services this request's kontainer held instances of — the request-specific part only.
 *
 * The definitions are static per blueprint, so `InsightsApi.GetKontainerGraph` serves them once instead
 * of every record repeating them; a reader joins on [Data.Service.cls].
 *
 * VUE-REF: `reference/collectors/KontainerCollector.kt`
 */
class KontainerCollector(
    private val kontainer: Kontainer,
    private val blueprint: KontainerBlueprint,
    private val graphs: InsightsGraphs,
) : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "kontainer"

        /** A kontainer alive longer than this counts as old. */
        private const val OLD_AFTER_SECONDS = 15
    }

    /**
     * The slice. **A wire break**: records written before 2026-10-19 carry every definition under
     * `info.services` instead, and are still served as written.
     */
    data class Data(
        val numOld: Int,
        val numTotal: Int,
        /** [InsightsKontainerGraph.fingerprint] of the graph [services] join against; null when it could not be built. */
        val graphFingerprint: String?,
        /** Only the services with at least one instance. */
        val services: List<Service>,
    ) : InsightsCollectorData {

        /** A service with its instances. */
        data class Service(
            /** The FQN the service is registered under: the join key into [InsightsKontainerGraph.services]. */
            val cls: String,
            /** As provided in THIS kontainer, so `DynamicOverride` when the instance was handed in at creation. */
            val type: ServiceProvider.Type,
            val instances: List<Instance>,
        )

        /** One instance; Slumber writes [createdAt] as `{ts, timezone, human}`, `ts` in epoch millis. */
        data class Instance(
            val cls: String,
            val createdAt: Instant,
        )

        companion object {
            /**
             * Keeps only the [providers] that have instances.
             *
             * Reads the providers rather than `getDebugInfo()`, which would build every definition only for
             * them to be dropped. Names go through [DebugInfo.ClassInfo] so the join key is exactly the graph's.
             */
            fun of(
                numOld: Int,
                numTotal: Int,
                graphFingerprint: String?,
                providers: Map<KClass<*>, ServiceProvider>,
            ) = Data(
                numOld = numOld,
                numTotal = numTotal,
                graphFingerprint = graphFingerprint,
                services = providers
                    .filterValues { it.instances.isNotEmpty() }
                    .map { (cls, provider) ->
                        Service(
                            cls = DebugInfo.ClassInfo.of(cls).fqn,
                            type = provider.type,
                            instances = provider.instances.map {
                                Instance(cls = DebugInfo.ClassInfo.of(it.instance::class).fqn, createdAt = it.createdAt)
                            },
                        )
                    },
            )
        }
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    override fun finish(call: ApplicationCall): Data {
        // Separately, so a graph that cannot be built still leaves the request's own services in the record
        val graphFingerprint = try {
            graphs.kontainerGraph.fingerprint
        } catch (e: Throwable) {
            println("[ERROR] Could not build the kontainer graph!\n${e.stackTraceToString()}")
            null
        }

        return try {
            Data.of(
                // TODO: make the threshold configurable
                numOld = blueprint.tracker.getNumAlive(OLD_AFTER_SECONDS),
                numTotal = blueprint.tracker.getNumAlive(),
                graphFingerprint = graphFingerprint,
                providers = kontainer.tools.factory.getAllProviders(),
            )
        } catch (e: Throwable) {
            println("[ERROR] Could not read the kontainer's services!\n${e.stackTraceToString()}")

            Data(numOld = 0, numTotal = 0, graphFingerprint = graphFingerprint, services = emptyList())
        }
    }
}
