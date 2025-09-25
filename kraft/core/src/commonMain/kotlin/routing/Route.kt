package de.peekandpoke.kraft.routing

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
