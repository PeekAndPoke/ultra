package de.peekandpoke.ktorfx.insights

import de.peekandpoke.ktorfx.core.fullUrl
import de.peekandpoke.ktorfx.insights.collectors.RoutingCollector
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Applies insights instrumentation to the Pipeline
 */
fun Route.instrumentWithInsights() {

    // Install the tracer
    RoutingInstrumentation {
        getTopMostRouting()?.registerTracer()
    }

    val plugin = createRouteScopedPlugin(name = "KtorFX-Insights") {

        val timer = AttributeKey<Long>("StartTime")

        on(CallSetup) { call ->
            call.attributes.put(timer, System.nanoTime())

            call.ktorFxInsights?.start(call)
        }

        on(ResponseSent) { call ->
            call.attributes.getOrNull(timer)?.let { startTime ->
                val ns = System.nanoTime() - startTime

                application.log.trace("${call.request.fullUrl()} took ${ns / 1_000_000.0} ms")
            }

            // Record the collected insights
            call.ktorFxInsights?.let { insights ->
                call.launch(Dispatchers.Unconfined) { insights.finish(call) }
            }
        }
    }

    install(plugin)
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

