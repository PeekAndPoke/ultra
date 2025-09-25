package de.peekandpoke.kraft.routing

/**
 * A static route is a route that does not have any route parameters
 */
open class Static(pattern: String) : RouteBase(pattern = pattern, numParams = 0) {
    operator fun invoke() =
        buildUri()
}

/**
 * A parameterized route with one route parameter
 */
open class Route1(pattern: String) : RouteBase(pattern = pattern, numParams = 1) {
    /** Builds the uri with the given parameters */
    fun build(p1: String) =
        buildUri(p1)
}

/**
 * A parameterized route with two route parameters
 */
open class Route2(pattern: String) : RouteBase(pattern = pattern, numParams = 2) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String) =
        buildUri(p1, p2)
}

/**
 * A parameterized route with two route parameters
 */
open class Route3(pattern: String) : RouteBase(pattern = pattern, numParams = 3) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String, p3: String) =
        buildUri(p1, p2, p3)
}

/**
 * A parameterized route with two route parameters
 */
open class Route4(pattern: String) : RouteBase(pattern = pattern, numParams = 4) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String, p3: String, p4: String) =
        buildUri(p1, p2, p3, p4)
}

/**
 * A parameterized route with two route parameters
 */
open class Route5(pattern: String) : RouteBase(pattern = pattern, numParams = 5) {
    /** Builds the uri with the given parameters */
    fun build(p1: String, p2: String, p3: String, p4: String, p5: String) =
        buildUri(p1, p2, p3, p4, p5)
}
