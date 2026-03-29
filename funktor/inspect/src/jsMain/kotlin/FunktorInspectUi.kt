package io.peekandpoke.funktor.inspect

import io.peekandpoke.funktor.cluster.FunktorClusterApiClient
import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.funktor.inspect.introspection.IntrospectionUi
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiClient
import io.peekandpoke.funktor.logging.LoggingUi
import io.peekandpoke.funktor.logging.api.LoggingApiClient
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.html.RenderFn
import kotlinx.html.Tag

class FunktorInspectUi(
    loggingApi: LoggingApiClient,
    clusterApi: FunktorClusterApiClient,
    introspectionApi: IntrospectionApiClient,
    kronos: () -> Kronos = { Kronos.systemUtc },
    customInternals: RenderFn = {},
    val routes: FunktorInspectRoutes = FunktorInspectRoutes(),
) {
    val logging = LoggingUi(
        api = loggingApi,
        routes = routes.logging,
    )

    val cluster = FunktorClusterUi(
        api = clusterApi,
        routes = routes.cluster,
        kronos = kronos,
        customInternals = customInternals,
    )

    val introspection = IntrospectionUi(
        api = introspectionApi,
        routes = routes.app,
    )

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
