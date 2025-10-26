package de.peekandpoke.funktor.insights.gui

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.core.broker.Routes
import de.peekandpoke.funktor.core.broker.TypedRoute

class InsightsGuiRoutes(converter: OutgoingConverter) : Routes(converter, "/_") {

    data class PathParam(
        val bucket: String,
        val file: String,
    ) {
        val path = "$bucket/$file"
    }

    val bar = route<PathParam>("/insights/bar/{bucket}/{file}")

    val details = route<PathParam>("/insights/details/{bucket}/{file}")

    fun details(bucket: String, path: String): TypedRoute.Bound<PathParam> {
        return details.bind(PathParam(bucket = bucket, file = path))
    }
}
