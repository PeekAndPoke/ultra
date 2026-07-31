package io.peekandpoke.funktor.insights

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingPipelineCall
import io.ktor.server.routing.RoutingResolveTrace
import io.ktor.server.routing.Routing
import io.ktor.util.AttributeKey
import io.ktor.server.request.path
import io.peekandpoke.funktor.core.fullUrl
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.rest.FunktorRouteAttributes
import io.peekandpoke.funktor.rest.InsightsLevel
import io.peekandpoke.funktor.rest.InsightsOptions
import io.peekandpoke.funktor.rest.InsightsOptionsKey
import io.peekandpoke.funktor.insights.api.InsightsApi
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
                val options = when {
                    // Second layer, restored 2026-07-31. The route attribute is the primary mechanism
                    // and covers every funktor ApiRoute — but a request that resolves to something else
                    // (a `fallback { }` catch-all, a wrong method, an unmatched sub-path) carries NO
                    // attribute bag and therefore defaults to FULL. That let an unauthenticated
                    // `POST /_/funktor/insights/records` write a ~270 KB record of the insights API,
                    // which the deleted `isExcluded` used to suppress.
                    //
                    // Narrower than what it replaces on purpose: matched against the DECODED path only,
                    // never the query string, so neither of the two bypasses that killed `isExcluded`
                    // (percent-encoding, and `?next=/_/funktor/insights`) has anywhere to act.
                    call.request.path().startsWith(InsightsApi.base) -> InsightsOptions.off

                    else -> call.insightsOptions()
                }

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
 * The mounting code copies the route's attributes onto the resolved ktor node
 * (`FunktorRouteAttributes`), so the options are readable from the call.
 *
 * **ktor has two routing call types and both must be handled.** At the `ResponseSent` hook the call is a
 * `RoutingPipelineCall`; *inside a route handler* it is a [RoutingCall], which is a different class and
 * not a subtype. Matching only the first made this silently return the default whenever a collector was
 * driven from a handler — `dropQueryParams` failed open there, and no test noticed because none reached
 * the branch. Found by probing the actual runtime type, 2026-07-31.
 *
 * Anything else (static resources, unmatched paths) carries no route attributes and so records in full.
 */
fun ApplicationCall.insightsOptions(): InsightsOptions {
    val node = when (this) {
        is RoutingPipelineCall -> route
        is RoutingCall -> route
        else -> null
    }

    return node?.attributes?.getOrNull(FunktorRouteAttributes)?.get(InsightsOptionsKey)
        ?: InsightsOptions.default
}

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
