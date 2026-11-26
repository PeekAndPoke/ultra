package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.InsightsGraphs
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.funktor.insights.wireProviderType
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import java.time.Instant
import kotlin.reflect.KClass

/**
 * The services this request's kontainer had created instances of when the request finished: a count and the
 * first and last creation time per service, no instances.
 *
 * Not all of it is request-specific: a `Singleton` entry is process state, shared by every request and
 * created by the boot or by whichever request first needed it; every other type is this kontainer's own.
 * Its creation time only says whether this request was the one that created it. The definitions are static
 * per blueprint, so `InsightsApiClient.GetKontainerGraph` serves them once instead of every record repeating
 * them; a reader joins on [Data.Service.cls].
 */
class KontainerCollector(
    private val kontainer: Kontainer,
    private val graphs: InsightsGraphs,
) : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "kontainer"

        /**
         * Builds the slice, and never throws: a collector that throws drops the WHOLE record.
         *
         * Two tries, not one, so a graph that cannot be built leaves `graphFingerprint` null and keeps the
         * request's services.
         */
        @Suppress("detekt:TooGenericExceptionCaught")
        internal fun collect(
            graphFingerprint: () -> String,
            providers: () -> Map<KClass<*>, ServiceProvider>,
        ): Data {
            val fingerprint = try {
                graphFingerprint()
            } catch (e: Throwable) {
                println("[ERROR] Could not build the kontainer graph!\n${e.stackTraceToString()}")
                null
            }

            return try {
                Data.of(graphFingerprint = fingerprint, providers = providers())
            } catch (e: Throwable) {
                println("[ERROR] Could not read the kontainer's services!\n${e.stackTraceToString()}")

                Data(graphFingerprint = fingerprint, services = emptyList())
            }
        }
    }

    /**
     * The slice. **Two wire breaks**: records written before 2026-10-19 carry every definition under
     * `info.services`, and records written before the counters (ULTRA-00329) carry an `instances` list per
     * service instead of [Service.count]. Both are still served as written.
     *
     * No `numOld`/`numTotal`, which those records still have (maintainer, 2026-10-19): they counted tracked
     * kontainers, and tracking (`KontainerBlueprint.Config.trackKontainers`) cannot be switched on from
     * outside `ultra:kontainer`, so they were always 0.
     */
    data class Data(
        /** [InsightsKontainerGraph.fingerprint] of the graph [services] join against; null when it could not be built. */
        val graphFingerprint: String?,
        /** Only the services with at least one creation. */
        val services: List<Service>,
    ) : InsightsCollectorData {

        /**
         * A service with what its provider created.
         *
         * Slumber writes both times as `{ts, timezone, human}`, `ts` in epoch millis. The counters default to no
         * creations, so a service from a record written before them reads as one without creations.
         */
        data class Service(
            /** The FQN the service is registered under: the join key into [InsightsKontainerGraph.services]. */
            val cls: String,
            /**
             * As provided in THIS kontainer, so `DynamicOverride` when the instance was handed in at creation.
             * One of the five wire names of [wireProviderType], unchanged since before the type and kind split.
             */
            val type: String,
            /** How many instances the provider created; for a `Singleton` that count is process-wide */
            val count: Long = 0,
            val firstCreatedAt: Instant? = null,
            val lastCreatedAt: Instant? = null,
        )

        companion object {
            /**
             * Keeps only the [providers] that created at least one instance.
             *
             * Reads the providers rather than `getDebugInfo()`, which would build every definition only for
             * them to be dropped. Names go through [DebugInfo.ClassInfo] so the join key is exactly the graph's.
             */
            fun of(
                graphFingerprint: String?,
                providers: Map<KClass<*>, ServiceProvider>,
            ) = Data(
                graphFingerprint = graphFingerprint,
                services = providers.mapNotNull { (cls, provider) ->
                    // One snapshot per provider, so the count and both times belong together
                    val created = provider.created.takeIf { it.count > 0 } ?: return@mapNotNull null

                    Service(
                        cls = DebugInfo.ClassInfo.of(cls).fqn,
                        type = wireProviderType(provider.type, provider.kind, provider.overridden),
                        count = created.count,
                        firstCreatedAt = created.firstAt,
                        lastCreatedAt = created.lastAt,
                    )
                },
            )
        }
    }

    override fun finish(call: ApplicationCall): Data = collect(
        graphFingerprint = { graphs.kontainerGraph.fingerprint },
        providers = { kontainer.tools.factory.getAllProviders() },
    )
}
