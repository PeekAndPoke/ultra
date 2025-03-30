package de.peekandpoke.kraft.addons.routing

import de.peekandpoke.ultra.common.decodeUriComponent
import de.peekandpoke.ultra.common.encodeUriComponent

/**
 * Common Route representation
 */
interface Route {

    interface Renderable {
        /**
         * Builds a uri with the given [routeParams]
         */
        fun buildUri(vararg routeParams: String): String

        /**
         * Builds a uri with the given [routeParams] and [queryParams]
         */
        fun buildUri(routeParams: Map<String, String>, queryParams: Map<String, String?>): String
    }

    /**
     * Represents a route match
     */
    data class Match(
        /** The route that was matched */
        val route: Route,
        /** Route params extract from url placeholder */
        val routeParams: Map<String, String>,
        /** Query params after like ?p=1&t=2 */
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
        operator fun get(key: String) = param(key)

        /** Gets the route param with the given [name] or defaults to [default] */
        fun param(name: String, default: String = ""): String = routeParams[name] ?: default

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

//    /**
//     * Internal helper for building uris
//     */
//    fun String.replacePlaceholder(placeholder: String, value: String) =
//        replace("{$placeholder}", value.encodeUriComponent())
//
//    /**
//     * Builds a uri with the given [routeParams]
//     */
//    fun buildUri(vararg routeParams: String): String
//
//    /**
//     * Builds a uri with the given [routeParams] and [queryParams]
//     */
//    fun buildUri(routeParams: Map<String, String>, queryParams: Map<String, String>): String
}

/**
 * A parameterized route with one route parameter
 */
abstract class RouteBase(final override val pattern: String, numParams: Int) : Route, Route.Renderable {

    companion object {
        @Suppress("RegExpRedundantEscape")
        val placeholderRegex = "\\{([^}]*)\\}".toRegex()
        const val extractRegexPattern = "([^/]*)"
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
            else -> arrayOf(uri.substring(0, queryIdx), uri.substring(queryIdx + 1))
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
    override fun buildUri(vararg routeParams: String): String =
        routeParams.foldIndexed("#$pattern") { idx, pattern, param ->
            pattern.replacePlaceholder(placeholders[idx], param)
        }

    /**
     * Builds a uri with the given [routeParams] and [queryParams]
     */
    override fun buildUri(routeParams: Map<String, String>, queryParams: Map<String, String?>): String {

        val withoutQuery = routeParams.entries.fold("#$pattern") { pattern, entry ->
            pattern.replacePlaceholder(entry.key, entry.value)
        }

        @Suppress("UNCHECKED_CAST")
        val cleanedQueryParams = queryParams.filterValues { it != null } as Map<String, String>

        return when (cleanedQueryParams.isEmpty()) {
            true -> withoutQuery
            else -> "$withoutQuery?" + cleanedQueryParams
                .map { "${it.key}=${it.value.encodeUriComponent()}" }.joinToString("&")
        }
    }

    /**
     * Internal helper for building uris
     */
    private fun String.replacePlaceholder(placeholder: String, value: String) =
        replace("{$placeholder}", value.encodeUriComponent())
}

/**
 * A static route is a route that does not have any route parameters
 */
open class Static(pattern: String) : RouteBase(pattern, 0) {
    operator fun invoke() = buildUri()
}

/**
 * A route that matches a regex, f.e. used as a fallback route.
 */
open class Pattern(private val regex: Regex) : Route {

    companion object {
        val CatchAll = Pattern(".*".toRegex())
    }

    override val pattern: String
        get() = regex.pattern

    override fun match(uri: String): Route.Match? {
        return regex.matchEntire(uri)?.let {
            Route.Match(
                route = this,
                routeParams = emptyMap(),
                queryParams = emptyMap(),
            )
        }
    }
}

/**
 * A parameterized route with one route parameter
 */
open class Route1(pattern: String) : RouteBase(pattern, 1) {
    /** Builds the uri with the given parameters */
    fun build(p1: String) = buildUri(p1)
}

/**
 * A parameterized route with two route parameter
 */
open class Route2(pattern: String) : RouteBase(pattern, 2) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String) = buildUri(p1, p2)
}

/**
 * A parameterized route with two route parameter
 */
open class Route3(pattern: String) : RouteBase(pattern, 3) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String, p3: String) = buildUri(p1, p2, p3)
}

/**
 * A parameterized route with two route parameter
 */
open class Route4(pattern: String) : RouteBase(pattern, 4) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String, p3: String, p4: String) = buildUri(p1, p2, p3, p4)
}

/**
 * A parameterized route with two route parameter
 */
open class Route5(pattern: String) : RouteBase(pattern, 5) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String, p3: String, p4: String, p5: String) = buildUri(p1, p2, p3, p4, p5)
}
