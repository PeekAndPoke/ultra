package de.peekandpoke.funktor.core.broker

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import io.ktor.http.*
import kotlin.reflect.KParameter

/**
 * Base class for all typed route representation
 */
sealed class TypedRoute<PARAMS> {

    /** Reference to the [OutgoingConverter] */
    abstract val converter: OutgoingConverter

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
        override val converter: OutgoingConverter,
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
                converter = converter,
                paramsType = kType(),
                pattern = pattern,
            )
    }

    /**
     * Route with input params
     */
    data class Sse<PARAMS>(
        override val converter: OutgoingConverter,
        override val paramsType: TypeRef<PARAMS>,
        override val pattern: UriPattern,
    ) : TypedRoute<PARAMS>() {
        init {
            validateParams()
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
        override val converter: OutgoingConverter,
        override val paramsType: TypeRef<PARAMS>,
        override val pattern: UriPattern,
    ) : TypedRoute<PARAMS>() {
        init {
            validateParams()
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
        override val converter: OutgoingConverter,
        override val paramsType: TypeRef<PARAMS>,
        val bodyType: TypeRef<BODY>,
        override val pattern: UriPattern,
    ) : TypedRoute<PARAMS>() {
        init {
            validateParams()
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
     * Validates all params of the route.
     *
     * - The [OutgoingConverter] must be able to handle all parameter types.
     * - All properties of the parameter class without default value must be found in the [pattern].
     */
    protected fun validateParams() {
        // Check that all properties of the route object can be handled by the outgoing converter
        val unhandled = reifiedParamsType.ctorParams2Types.filter { !converter.canHandle(it.second) }

        if (unhandled.isNotEmpty()) {
            throw InvalidRouteParamsException(
                "Route '${pattern.pattern}': The outgoing converter cannot handle parameters " +
                        "${unhandled.map { it.first.name }} of route object '${reifiedParamsType.cls}'"
            )
        }

        // Check that all non optional parameters are present in the route
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
