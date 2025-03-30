package de.peekandpoke.ktorfx.insights.gui

import de.peekandpoke.ktorfx.cluster.depot.domain.DepotItem
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.core.broker.Routes
import de.peekandpoke.ktorfx.core.broker.TypedRoute

class InsightsGuiRoutes(converter: OutgoingConverter) : Routes(converter, "/_") {

    data class PathParam(val path: String)

    val bar = route<PathParam>("/insights/bar/{path}")

    val details = route<PathParam>("/insights/details/{path}/show")

    fun details(path: String): TypedRoute.Bound<PathParam> {
        return details.bind(PathParam(path))
    }

    fun details(file: DepotItem): TypedRoute.Bound<PathParam> {
        return details(file.path)
    }
}
