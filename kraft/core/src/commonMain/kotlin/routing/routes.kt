package de.peekandpoke.kraft.routing

@Suppress("UNCHECKED_CAST")
internal fun Map<String, String?>.removeNullValues() = filterValues { it != null } as Map<String, String>

/**
 * A static route is a route that does not have any route parameters
 */
class Static(pattern: String) : RouteBase(pattern = pattern, numParams = 0) {
    /** Binds the uri with the given parameters */
    operator fun invoke(): Route.Bound {
        return bind()
    }
}

/**
 * A parameterized route with one route parameter
 */
class Route1(pattern: String) : RouteBase(pattern = pattern, numParams = 1) {
    /** Binds the uri with the given parameters */
    operator fun invoke(p1: String): Route.Bound {
        return bind(p1)
    }
}

/**
 * A parameterized route with two route parameters
 */
class Route2(pattern: String) : RouteBase(pattern = pattern, numParams = 2) {
    /** Binds the uri with the given parameters */
    operator fun invoke(p1: String, p2: String): Route.Bound {
        return bind(p1, p2)
    }
}

/**
 * A parameterized route with three route parameters
 */
class Route3(pattern: String) : RouteBase(pattern = pattern, numParams = 3) {
    /** Binds the uri with the given parameters */
    operator fun invoke(p1: String, p2: String, p3: String): Route.Bound {
        return bind(p1, p2, p3)
    }
}

/**
 * A parameterized route with four route parameters
 */
class Route4(pattern: String) : RouteBase(pattern = pattern, numParams = 4) {
    /** Builds the uri with the given parameters */
    operator fun invoke(p1: String, p2: String, p3: String, p4: String): Route.Bound {
        return bind(p1, p2, p3, p4)
    }
}

/**
 * A parameterized route with five route parameters
 */
class Route5(pattern: String) : RouteBase(pattern = pattern, numParams = 5) {
    /** Builds the uri with the given parameters */
    operator fun invoke(p1: String, p2: String, p3: String, p4: String, p5: String): Route.Bound {
        return bind(p1, p2, p3, p4, p5)
    }
}

/**
 * A parameterized route with six route parameters
 */
class Route6(pattern: String) : RouteBase(pattern = pattern, numParams = 6) {
    /** Builds the uri with the given parameters */
    operator fun invoke(p1: String, p2: String, p3: String, p4: String, p5: String, p6: String): Route.Bound {
        return bind(p1, p2, p3, p4, p5, p6)
    }
}

/**
 * A parameterized route with six route parameters
 */
class Route7(pattern: String) : RouteBase(pattern = pattern, numParams = 7) {
    /** Builds the uri with the given parameters */
    operator fun invoke(
        p1: String,
        p2: String,
        p3: String,
        p4: String,
        p5: String,
        p6: String,
        p7: String,
    ): Route.Bound {
        return bind(p1, p2, p3, p4, p5, p6, p7)
    }
}
