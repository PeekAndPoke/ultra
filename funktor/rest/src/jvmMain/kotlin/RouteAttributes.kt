package io.peekandpoke.funktor.rest

import io.peekandpoke.ultra.common.TypedKey

/**
 * How much of a request the insights recorder writes.
 *
 * Declared per route through [RecordInsights]; absent means [FULL].
 */
enum class InsightsLevel {
    /** Record nothing at all. */
    OFF,

    /** Record only the headline — method, uri, status, timing — and no collector slices. */
    BRIEF,

    /** Record the headline and every collector's slice. */
    FULL,
}

/**
 * The insights recording level for a route.
 *
 * **The first route attribute read at REQUEST time.** The others — `docs`, `codeGen`, `security` — are
 * build-time metadata that never reaches a running request. For this one to be visible, the mounting
 * code copies the route's whole [io.peekandpoke.ultra.common.TypedAttributes] onto the ktor route (see
 * `FunktorRouteAttributes` in `routing.kt`), and the insights plugin reads it back off the resolved
 * route.
 *
 * Declaring it on the route rather than matching the request URI is the point: a URI is client-controlled
 * and a substring test over it could be defeated by percent-encoding the path or by putting the pattern
 * in a query string.
 */
val RecordInsights = TypedKey<InsightsLevel>("RecordInsights")

/** The route's [InsightsLevel]. Absent means [InsightsLevel.FULL]. */
val ApiRoute<*>.insightsLevel: InsightsLevel get() = attributes[RecordInsights] ?: InsightsLevel.FULL

//  attr(key, value) — a readable alias for withAttribute, so the chain stays declarative  ///////////

/** Sets a route attribute. */
fun <RESPONSE, T : Any> ApiRoute.Plain<RESPONSE>.attr(key: TypedKey<T>, value: T) =
    withAttribute(key, value)

/** Sets a route attribute. */
fun <PARAMS, T : Any> ApiRoute.Sse<PARAMS>.attr(key: TypedKey<T>, value: T) =
    withAttribute(key, value)

/** Sets a route attribute. */
fun <PARAMS, RESPONSE, T : Any> ApiRoute.WithParams<PARAMS, RESPONSE>.attr(key: TypedKey<T>, value: T) =
    withAttribute(key, value)

/** Sets a route attribute. */
fun <BODY, RESPONSE, T : Any> ApiRoute.WithBody<BODY, RESPONSE>.attr(key: TypedKey<T>, value: T) =
    withAttribute(key, value)

/** Sets a route attribute. */
fun <PARAMS, BODY, RESPONSE, T : Any> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.attr(
    key: TypedKey<T>,
    value: T,
) = withAttribute(key, value)

//  noInsights() — shorthand for the common case  ////////////////////////////////////////////////////

/** Shorthand for `attr(RecordInsights, InsightsLevel.OFF)`. */
fun <RESPONSE> ApiRoute.Plain<RESPONSE>.noInsights() = attr(RecordInsights, InsightsLevel.OFF)

/** Shorthand for `attr(RecordInsights, InsightsLevel.OFF)`. */
fun <PARAMS> ApiRoute.Sse<PARAMS>.noInsights() = attr(RecordInsights, InsightsLevel.OFF)

/** Shorthand for `attr(RecordInsights, InsightsLevel.OFF)`. */
fun <PARAMS, RESPONSE> ApiRoute.WithParams<PARAMS, RESPONSE>.noInsights() =
    attr(RecordInsights, InsightsLevel.OFF)

/** Shorthand for `attr(RecordInsights, InsightsLevel.OFF)`. */
fun <BODY, RESPONSE> ApiRoute.WithBody<BODY, RESPONSE>.noInsights() =
    attr(RecordInsights, InsightsLevel.OFF)

/** Shorthand for `attr(RecordInsights, InsightsLevel.OFF)`. */
fun <PARAMS, BODY, RESPONSE> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.noInsights() =
    attr(RecordInsights, InsightsLevel.OFF)
