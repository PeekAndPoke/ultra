package io.peekandpoke.funktor.core.broker

import io.ktor.http.*
import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import kotlin.reflect.KParameter

/**
 * Base class for all typed route representation
 */
sealed class TypedRoute<PARAMS> {

    /** TypeRefs of all route and query parameters */
    abstract val paramsType: TypeRef<PARAMS>

    /** The uri */
    abstract val pattern: UriPattern

    /**
     * Reified versions of all parameter types
     */
    val reifiedParamsType by lazy { ReifiedKType(paramsType.type) }

    /**
     * All parameter names parsed from the [pattern]
     */
    val parsedUriParams by lazy {
        "\\{([^}]*)}".toRegex()
            .findAll(pattern.pattern)
            .map { it.groupValues[1] }
            // remove the special ... suffix on ktor routes
            .map { it.removeSuffix("...") }
            .toList()
    }

    /**
     * List of all parameters (uri params and query params)
     */
    val paramNames: List<KParameter> by lazy {
        reifiedParamsType.ctorParams2Types.map { it.first }
    }

    /**
     * A typed route bound to a parameters object
     */
    data class Bound<T>(val route: TypedRoute<T>, val obj: T)

    /**
     * Plain route without input params or input body
     */
    data class Plain(
        override val pattern: UriPattern,
    ) : TypedRoute<Unit>() {
        override val paramsType: TypeRef<Unit> = TypeRef.Unit

        /**
         * Creates a [Bound] route
         */
        operator fun invoke() = bind()

        /**
         * Creates a [Bound] route
         */
        fun bind() = Bound(this, Unit)

        /**
         * Upgrades the plain route to a parameterized route.
         */
        inline fun <reified PARAMS> withParams(): WithParams<PARAMS> =
            WithParams(
                paramsType = kType(),
                pattern = pattern,
            )
    }

    /**
     * Route with input params
     */
    data class Sse<PARAMS>(
        override val paramsType: TypeRef<PARAMS>,
        override val pattern: UriPattern,
    ) : TypedRoute<PARAMS>() {
        init {
            validateUriPattern()
        }

        /**
         * Creates a [Bound] route
         */
        operator fun invoke(obj: PARAMS) = bind(obj)

        /**
         * Creates a [Bound] route
         */
        fun bind(obj: PARAMS) = Bound(this, obj)
    }

    /**
     * Route with input params
     */
    data class WithParams<PARAMS>(
        override val paramsType: TypeRef<PARAMS>,
        override val pattern: UriPattern,
    ) : TypedRoute<PARAMS>() {
        init {
            validateUriPattern()
        }

        /**
         * Creates a [Bound] route
         */
        operator fun invoke(obj: PARAMS) = bind(obj)

        /**
         * Creates a [Bound] route
         */
        fun bind(obj: PARAMS) = Bound(this, obj)
    }

    /**
     * Route with input params and input body
     */
    data class WithParamsAndBody<PARAMS, BODY>(
        override val paramsType: TypeRef<PARAMS>,
        val bodyType: TypeRef<BODY>,
        override val pattern: UriPattern,
    ) : TypedRoute<PARAMS>() {
        init {
            validateUriPattern()
        }

        /**
         * Creates a [Bound] route
         */
        operator fun invoke(obj: PARAMS) = bind(obj)

        /**
         * Creates a [Bound] route
         */
        fun bind(obj: PARAMS) = Bound(this, obj)
    }

    /**
     * Renders the route by replacing the placeholders with the given [parameters]
     */
    fun render(parameters: List<Pair<String, String>>): String {
        return parameters.fold(pattern.pattern) { acc, (k, v) ->
            acc.replace("{$k}", v.encodeURLQueryComponent(encodeFull = true))
        }
    }

    /**
     * Validates that the [OutgoingConverter] can handle all parameter types of this route.
     */
    fun validateConverterCompatibility(converter: OutgoingConverter) {
        val unhandled = reifiedParamsType.ctorParams2Types.filter { !converter.canHandle(it.second) }

        if (unhandled.isNotEmpty()) {
            throw InvalidRouteParamsException(
                "Route '${pattern.pattern}': The outgoing converter cannot handle parameters " +
                        "${unhandled.map { it.first.name }} of route object '${reifiedParamsType.cls}'"
            )
        }
    }

    /**
     * Validates that all non-optional parameters are present in the URI [pattern].
     */
    protected fun validateUriPattern() {
        val params: List<KParameter> = reifiedParamsType.ctor?.parameters ?: emptyList()

        val missingInUri = params
            .filter { !it.isOptional }
            .map { it.name }
            .filter { !parsedUriParams.contains(it) }

        if (missingInUri.isNotEmpty()) {
            error("Route '${pattern.pattern}': misses route parameters $missingInUri for route object '${reifiedParamsType.cls}'")
        }
    }
}
