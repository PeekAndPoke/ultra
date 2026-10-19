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
 * The services this request's kontainer held instances of when the request finished.
 *
 * Not all of it is request-specific: a `Singleton` entry is process state, shared by every request and
 * created by the boot or by whichever request first needed it; every other type is this kontainer's own.
 * `createdAt` only says whether this request was the one that created it. The definitions are static
 * per blueprint, so `InsightsApi.GetKontainerGraph` serves them once instead of every record repeating
 * them; a reader joins on [Data.Service.cls].
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

        /**
         * Builds the slice, and never throws: a collector that throws drops the WHOLE record.
         *
         * Two tries, not one, so a graph that cannot be built leaves `graphFingerprint` null and keeps the
         * request's services. [numTotal] is read before [numOld], so a kontainer collected in between cannot
         * make `numOld` exceed `numTotal`.
         */
        @Suppress("detekt:TooGenericExceptionCaught")
        internal fun collect(
            graphFingerprint: () -> String,
            numTotal: () -> Int,
            numOld: () -> Int,
            providers: () -> Map<KClass<*>, ServiceProvider>,
        ): Data {
            val fingerprint = try {
                graphFingerprint()
            } catch (e: Throwable) {
                println("[ERROR] Could not build the kontainer graph!\n${e.stackTraceToString()}")
                null
            }

            return try {
                val total = numTotal()

                Data.of(numOld = numOld(), numTotal = total, graphFingerprint = fingerprint, providers = providers())
            } catch (e: Throwable) {
                println("[ERROR] Could not read the kontainer's services!\n${e.stackTraceToString()}")

                Data(numOld = 0, numTotal = 0, graphFingerprint = fingerprint, services = emptyList())
            }
        }
    }

    /**
     * The slice. **A wire break**: records written before 2026-10-19 carry every definition under
     * `info.services` instead, and are still served as written.
     *
     * [numOld] and [numTotal] are always 0 in funktor: they count tracked kontainers, and tracking
     * (`KontainerBlueprint.Config.trackKontainers`) cannot be switched on from outside `ultra:kontainer`.
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

    override fun finish(call: ApplicationCall): Data = collect(
        graphFingerprint = { graphs.kontainerGraph.fingerprint },
        numTotal = { blueprint.tracker.getNumAlive() },
        // TODO: make the threshold configurable
        numOld = { blueprint.tracker.getNumAlive(OLD_AFTER_SECONDS) },
        providers = { kontainer.tools.factory.getAllProviders() },
    )
}
