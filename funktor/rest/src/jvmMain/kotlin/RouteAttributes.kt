package io.peekandpoke.funktor.rest

import io.peekandpoke.ultra.common.TypedKey

/**
 * How much of a request the insights recorder writes.
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
 * Per-route insights options.
 *
 * ```kotlin
 * SomeEndpoint.mount {
 *     docs { name = "…" }
 *         .insights { brief(); dropQueryParams() }
 *         .handle { … }
 * }
 * ```
 *
 * **The first route attribute read at REQUEST time.** `docs`, `codeGen` and `security` are build-time
 * metadata that never reaches a running request; for this one the mounting code copies the route's whole
 * [io.peekandpoke.ultra.common.TypedAttributes] onto the ktor route (`FunktorRouteAttributes` in
 * `routing.kt`), and the insights plugin reads it back off the resolved route.
 *
 * Declaring it on the route rather than matching the request URI is the point: a URI is client-controlled,
 * and the substring test this replaced could be defeated by percent-encoding the path or by putting the
 * pattern in a query string.
 */
data class InsightsOptions(
    val level: InsightsLevel,
    /**
     * Drop query parameters from the record entirely, rather than redacting them by name.
     *
     * The per-route hammer for an endpoint known to carry secrets in its query string — an OAuth
     * callback, a magic link. It is NOT the primary defence: every request's query parameters are
     * redacted by the same name-based policy as headers, because no one can tag every route.
     */
    val dropQueryParams: Boolean,
) {
    companion object {
        /** Record everything; redact query parameters by name rather than dropping them. */
        val default = InsightsOptions(level = InsightsLevel.FULL, dropQueryParams = false)
    }

    @RestDsl
    class Builder {
        private var level: InsightsLevel = InsightsLevel.FULL
        private var dropQueryParams: Boolean = false

        /** Record the headline and every collector slice. The default. */
        fun full() = apply { level = InsightsLevel.FULL }

        /** Record the headline only — method, uri, status, timing. No collector slices. */
        fun brief() = apply { level = InsightsLevel.BRIEF }

        /** Record nothing for this route. */
        fun off() = apply { level = InsightsLevel.OFF }

        /** Drop query parameters entirely instead of redacting them by name. */
        fun dropQueryParams() = apply { dropQueryParams = true }

        internal fun build() = InsightsOptions(level = level, dropQueryParams = dropQueryParams)
    }
}

val InsightsOptionsKey = TypedKey<InsightsOptions>("InsightsOptions")

/** The route's insights options; absent means [InsightsOptions.default]. */
val ApiRoute<*>.insights: InsightsOptions get() = attributes[InsightsOptionsKey] ?: InsightsOptions.default

//  insights { } — one builder per route variant, same shape as docs { } / codeGen { }  ///////////////

fun <RESPONSE> ApiRoute.Plain<RESPONSE>.insights(
    block: InsightsOptions.Builder.(ApiRoute.Plain<RESPONSE>) -> Unit,
) = withAttribute(InsightsOptionsKey, InsightsOptions.Builder().also { it.block(this) }.build())

fun <PARAMS> ApiRoute.Sse<PARAMS>.insights(
    block: InsightsOptions.Builder.(ApiRoute.Sse<PARAMS>) -> Unit,
) = withAttribute(InsightsOptionsKey, InsightsOptions.Builder().also { it.block(this) }.build())

fun <PARAMS, RESPONSE> ApiRoute.WithParams<PARAMS, RESPONSE>.insights(
    block: InsightsOptions.Builder.(ApiRoute.WithParams<PARAMS, RESPONSE>) -> Unit,
) = withAttribute(InsightsOptionsKey, InsightsOptions.Builder().also { it.block(this) }.build())

fun <BODY, RESPONSE> ApiRoute.WithBody<BODY, RESPONSE>.insights(
    block: InsightsOptions.Builder.(ApiRoute.WithBody<BODY, RESPONSE>) -> Unit,
) = withAttribute(InsightsOptionsKey, InsightsOptions.Builder().also { it.block(this) }.build())

fun <PARAMS, BODY, RESPONSE> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.insights(
    block: InsightsOptions.Builder.(ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>) -> Unit,
) = withAttribute(InsightsOptionsKey, InsightsOptions.Builder().also { it.block(this) }.build())

//  noInsights() — shorthand for the common case  ////////////////////////////////////////////////////

/** Shorthand for `insights { off() }`. */
fun <RESPONSE> ApiRoute.Plain<RESPONSE>.noInsights() = insights { off() }

/** Shorthand for `insights { off() }`. */
fun <PARAMS> ApiRoute.Sse<PARAMS>.noInsights() = insights { off() }

/** Shorthand for `insights { off() }`. */
fun <PARAMS, RESPONSE> ApiRoute.WithParams<PARAMS, RESPONSE>.noInsights() = insights { off() }

/** Shorthand for `insights { off() }`. */
fun <BODY, RESPONSE> ApiRoute.WithBody<BODY, RESPONSE>.noInsights() = insights { off() }

/** Shorthand for `insights { off() }`. */
fun <PARAMS, BODY, RESPONSE> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.noInsights() =
    insights { off() }

//  attr(key, value) — a readable alias for withAttribute  ///////////////////////////////////////////

fun <RESPONSE, T : Any> ApiRoute.Plain<RESPONSE>.attr(key: TypedKey<T>, value: T) = withAttribute(key, value)

fun <PARAMS, T : Any> ApiRoute.Sse<PARAMS>.attr(key: TypedKey<T>, value: T) = withAttribute(key, value)

fun <PARAMS, RESPONSE, T : Any> ApiRoute.WithParams<PARAMS, RESPONSE>.attr(key: TypedKey<T>, value: T) =
    withAttribute(key, value)

fun <BODY, RESPONSE, T : Any> ApiRoute.WithBody<BODY, RESPONSE>.attr(key: TypedKey<T>, value: T) =
    withAttribute(key, value)

fun <PARAMS, BODY, RESPONSE, T : Any> ApiRoute.WithBodyAndParams<PARAMS, BODY, RESPONSE>.attr(
    key: TypedKey<T>,
    value: T,
) = withAttribute(key, value)
