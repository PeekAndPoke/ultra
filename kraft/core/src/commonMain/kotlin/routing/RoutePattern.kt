package de.peekandpoke.kraft.routing

/**
 * A route that matches a regex, f.e. used as a fallback route.
 */
open class RoutePattern(private val regex: Regex) : Route {

    companion object {
        val CatchAll = RoutePattern(".*".toRegex())
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
