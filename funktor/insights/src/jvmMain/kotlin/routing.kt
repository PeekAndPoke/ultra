package io.peekandpoke.funktor.insights

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.peekandpoke.funktor.core.fullUrl
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.rest.FunktorRouteAttributes
import io.peekandpoke.funktor.rest.InsightsLevel
import io.peekandpoke.funktor.rest.InsightsOptions
import io.peekandpoke.funktor.rest.InsightsOptionsKey
import io.peekandpoke.funktor.insights.collectors.RoutingCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.microseconds

/**
 * Applies insights instrumentation to the Pipeline
 */
fun Route.instrumentWithInsights(config: InsightsConfig?) {

    if (config?.enabled != true) {
        return
    }

    // Install the tracer
    RoutingInstrumentation {
        getTopMostRouting()?.registerTracer()
    }

    val timer = AttributeKey<Long>("StartTime")

    val plugin = createRouteScopedPlugin(name = "Funktor-Insights") {

        on(CallSetup) { call ->
            call.funktorInsights?.let { insights ->
                call.attributes.put(timer, System.nanoTime())

                insights.start(call)
            }
        }

        on(ResponseSent) { call ->
            call.funktorInsights?.let { insights ->
                call.attributes.getOrNull(timer)?.let { startTime ->
                    val ns = System.nanoTime() - startTime

                    application.log.trace("${call.request.fullUrl()} took ${ns / 1_000_000.0} ms")
                }

                // Ask the RESOLVED ROUTE how much to record — never the request uri, which the client
                // controls. Resolving here rather than inside finish() also avoids launching a
                // coroutine for a request that is about to be discarded.
                val options = call.insightsOptions()

                if (options.level != InsightsLevel.OFF) {
                    call.launch(Dispatchers.IO) {
                        delay(1.microseconds)
                        insights.finish(call, options)
                    }
                }
            }
        }
    }

    install(plugin)
}

/**
 * The [InsightsOptions] of the route this call resolved to; [InsightsOptions.default] when there is none.
 *
 * A call handled by a funktor route is a `RoutingPipelineCall`, whose `route` is the resolved leaf
 * node — and the mounting code copied that route's attributes onto it (`FunktorRouteAttributes`).
 * Anything else (static resources, unmatched paths) has no route attributes and so records in full.
 */
fun ApplicationCall.insightsOptions(): InsightsOptions =
    (this as? RoutingPipelineCall)
        ?.route?.attributes?.getOrNull(FunktorRouteAttributes)
        ?.get(InsightsOptionsKey)
        ?: InsightsOptions.default

object RoutingInstrumentation {
    /**
     * AttributeKey used for storing tracing information related to the routing resolution process in an application.
     */
    val Key = AttributeKey<RoutingResolveTrace>("routing_resolve_trace")

    /**
     * Registry for remembering which [Routing] instances have already been instrumented. Used to avoid double instrumentation.
     */
    internal val Registry = mutableSetOf<Routing>()

    /**
     * Helper
     */
    internal operator fun <T> invoke(block: RoutingInstrumentation.() -> T): T = this.block()

    /**
     * Get the top most [Routing] parent
     */
    internal fun Route.getTopMostRouting(): Routing? {
        val found = mutableListOf<Routing>()

        var current: Route? = this

        while (current != null) {
            if (current is Routing) {
                found.add(current)
            }
            current = current.parent
        }

        return found.lastOrNull()
    }

    /**
     * We need some special handling for recording the route trace
     *
     * The problem:
     *
     * The route tracers are called before the pipeline is executed.
     * This mean that we did not have the chance to inject the kontainer
     * into the call parameters yet.
     *
     * The solution:
     *
     * We put the [RoutingResolveTrace] in the call attributes.
     * And the [RoutingCollector] will pick it up in its finish() method.
     *
     * NOTICE:
     *
     * This handler should only be registered once in the global routing { } block of the
     * application.
     */
    internal fun Routing.registerTracer() {
        if (!Registry.contains(this)) {
            Registry.add(this)

            this.trace {
                it.call.attributes.put(Key, it)
            }
        }
    }
}
