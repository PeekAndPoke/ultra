package de.peekandpoke.kraft.routing

import de.peekandpoke.ultra.common.decodeUriComponent
import de.peekandpoke.ultra.common.encodeUriComponent

/**
 * A parameterized route with one route parameter
 */
abstract class RouteBase(final override val pattern: String, numParams: Int) : Route {

    companion object {
        @Suppress("RegExpRedundantEscape")
        val placeholderRegex = "\\{([^}]+)\\}".toRegex()
        const val extractRegexPattern = "([^/]*)"

        /**
         * Internal helper for building uris
         */
        fun String.replacePlaceholder(placeholder: String, value: String) =
            replace("{$placeholder}", value.encodeUriComponent())
    }

    /**
     * We extract all placeholders from the pattern
     */
    private val placeholders = placeholderRegex
        .findAll(pattern)
        .map { it.groupValues[1] }
        .toList()

    /**
     * We construct a regex for matching the whole pattern with param placeholders
     */
    private val matchingRegex =
        placeholders.fold(pattern) { acc, placeholder ->
            acc.replace("{$placeholder}", extractRegexPattern)
        }.replace("/", "\\/").toRegex()

    init {
        // Sanity check
        if (numParams != placeholders.size) {
            error("The route '$pattern' has [${placeholders.size}] route-params but should have [$numParams]")
        }
    }

    /**
     * Tries to match the given [uri] against the [pattern] of the route.
     */
    override fun match(uri: String): Route.Match? {

        val (route, query) = when (val queryIdx = uri.indexOf("?")) {
            -1 -> arrayOf(uri, "")
            else -> arrayOf(
                uri.take(queryIdx),
                uri.substring(queryIdx + 1),
            )
        }

        val match = matchingRegex.matchEntire(route) ?: return null

//        console.log(placeholders)
//        console.log(match)
//        console.log(match.groupValues)

        val routeParams = placeholders
            .zip(match.groupValues.drop(1).map { it.decodeUriComponent() })
            .toMap()

        val queryParams = when (query.isEmpty()) {
            true -> emptyMap()
            else -> query.split("&").associate {
                when (val equalsIdx = it.indexOf("=")) {
                    -1 -> Pair(it, "")
                    else -> Pair(
                        it.substring(0, equalsIdx),
                        it.substring(equalsIdx + 1).decodeUriComponent(),
                    )
                }
            }
        }

        return Route.Match(route = this, routeParams = routeParams, queryParams = queryParams)
    }

    /**
     * Builds a uri with the given [routeParams]
     */
    fun bind(vararg routeParams: String?): Route.Bound {
        val routeParamsMap = routeParams
            .mapIndexed { idx, param -> placeholders[idx] to param }
            .toMap()

        return bind(routeParams = routeParamsMap, queryParams = emptyMap())
    }

    /**
     * Builds a uri with the given [routeParams] and [queryParams]
     */
    fun bind(routeParams: Map<String, String?>, queryParams: Map<String, String?>): Route.Bound {
        return Route.Bound(
            route = this,
            routeParams = routeParams.removeNullValues(),
            queryParams = queryParams.removeNullValues(),
        )
    }
}
