package io.peekandpoke.funktor.inspect

import io.peekandpoke.funktor.insights.api.InsightsApiClient
import io.peekandpoke.funktor.inspect.cluster.FunktorClusterApiClient
import io.peekandpoke.funktor.inspect.insights.InsightsUi
import io.peekandpoke.funktor.inspect.introspection.IntrospectionUi
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiClient
import io.peekandpoke.funktor.inspect.logging.LoggingUi
import io.peekandpoke.funktor.inspect.logging.api.LoggingApiClient
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.html.RenderFn
import kotlinx.html.Tag

class FunktorInspectUi(
    loggingApi: LoggingApiClient,
    clusterApi: FunktorClusterApiClient,
    introspectionApi: IntrospectionApiClient,
    /** The insights API; null leaves the insights pages out, as for an app without insights. */
    insightsApi: InsightsApiClient? = null,
    kronos: () -> Kronos = { Kronos.systemUtc },
    customInternals: RenderFn = {},
    val routes: FunktorInspectRoutes = FunktorInspectRoutes(),
) {
    val logging = LoggingUi(
        api = loggingApi,
        routes = routes.logging,
    )

    val cluster = io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi(
        api = clusterApi,
        routes = routes.cluster,
        kronos = kronos,
        customInternals = customInternals,
    )

    val introspection = IntrospectionUi(
        api = introspectionApi,
        routes = routes.app,
    )

    /** Recorded requests (ULTRA-00198); null without an [InsightsApiClient]. */
    val insights: InsightsUi? = insightsApi?.let {
        InsightsUi(api = it, routes = routes.insights)
    }

    operator fun invoke(block: FunktorInspectUi.() -> Unit) {
        this.block()
    }

    @Suppress("FunctionName")
    fun Tag.FunktorInspectOverviewPage() = comp(
        FunktorInspectOverviewPage.Props(ui = this@FunktorInspectUi)
    ) {
        FunktorInspectOverviewPage(it)
    }
}
