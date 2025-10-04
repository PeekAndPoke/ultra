package de.peekandpoke.kraft.routing

import de.peekandpoke.kraft.routing.RouteBase.Companion.replacePlaceholder
import de.peekandpoke.ultra.common.encodeUriComponent

/**
 * Common Route representation
 */
interface Route {

    interface Renderer {
        object Default : Renderer

        /** Builds the uri for the given [bound] route */
        fun render(bound: Bound): String = render(
            pattern = bound.route.pattern,
            routeParams = bound.routeParams,
            queryParams = bound.queryParams,
        )

        /** Builds the uri for the given [pattern], [routeParams] and [queryParams] */
        fun render(
            pattern: String,
            routeParams: Map<String, String>,
            queryParams: Map<String, String>,
        ): String {
            val withoutQuery = routeParams.entries.fold(pattern) { pattern, entry ->
                pattern.replacePlaceholder(entry.key, entry.value)
            }

            return when (queryParams.isEmpty()) {
                true -> withoutQuery
                else -> "$withoutQuery?" + queryParams
                    .map { "${it.key}=${it.value.encodeUriComponent()}" }
                    .joinToString("&")
            }
        }
    }

    /**
     * Represents a bound route
     */
    data class Bound(
        val route: Route,
        val routeParams: Map<String, String>,
        val queryParams: Map<String, String>,
    ) {
        /** Set [routeParams] */
        fun withRouteParams(vararg routeParams: Pair<String, String?>): Bound =
            withRouteParams(routeParams.toMap())

        /** Set [routeParams] */
        fun withRouteParams(routeParams: Map<String, String?>): Bound =
            copy(routeParams = routeParams.removeNullValues())

        /** Append [routeParams] */
        fun plusRouteParams(vararg routeParams: Pair<String, String?>): Bound =
            plusRouteParams(routeParams.toMap())

        /** Append [routeParams] */
        fun plusRouteParams(routeParams: Map<String, String?>): Bound =
            copy(routeParams = this.routeParams + routeParams.removeNullValues())

        /** Set [queryParams] */
        fun withQueryParams(vararg queryParams: Pair<String, String?>): Bound =
            withQueryParams(queryParams.toMap())

        /** Set [queryParams] */
        fun withQueryParams(queryParams: Map<String, String?>): Bound =
            copy(queryParams = queryParams.removeNullValues())

        /** Append [queryParams] */
        fun plusQueryParams(vararg queryParams: Pair<String, String?>): Bound =
            plusQueryParams(queryParams.toMap())

        /** Append [queryParams] */
        fun plusQueryParams(queryParams: Map<String, String?>): Bound =
            copy(queryParams = this.queryParams + queryParams.removeNullValues())
    }

    /**
     * Represents a route match
     */
    data class Match(
        /** The route that was matched */
        val route: Route,
        /** Route params extracted from uri placeholders */
        val routeParams: Map<String, String>,
        /** Query params, like ?p=1&t=2 */
        val queryParams: Map<String, String>,
    ) {
        companion object {
            val default = Match(route = Route.default, routeParams = emptyMap(), queryParams = emptyMap())
        }

        /** Shortcut to the pattern of the [route] */
        val pattern = route.pattern

        /** Combination of route and query params */
        val allParams = routeParams.plus(queryParams)

        /** Gets the route param with the given [key] / name */
        operator fun get(key: String) = routeParam(key)

        /** Gets the route param with the given [name] or defaults to [default] */
        fun routeParam(name: String, default: String = ""): String = routeParams[name] ?: default

        /** Gets the query param with the given [name] or defaults to [default] */
        fun queryParam(name: String, default: String = ""): String = queryParams[name] ?: default

        /** Returns a new instance with the query params set */
        fun withQueryParams(params: Map<String, String?>): Match {
            @Suppress("UNCHECKED_CAST")
            return copy(
                queryParams = params.filterValues { v -> v != null } as Map<String, String>
            )
        }

        /** Returns a new instance with the query params set */
        fun withQueryParams(vararg params: Pair<String, String?>): Match = withQueryParams(params.toMap())
    }

    companion object {
        /** Basic default route */
        val default = Static("")
    }

    /** The pattern of the route */
    val pattern: String

    /**
     * Matches the given [uri] against the [pattern] of the route.
     *
     * Returns an instance of [Match] or null if the pattern did not match.
     */
    fun match(uri: String): Match?
}
