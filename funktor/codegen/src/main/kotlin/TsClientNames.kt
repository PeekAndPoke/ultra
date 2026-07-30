package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.docs.codeGen

/**
 * Derives TypeScript identifiers from the names a route graph already carries.
 *
 * Everything here is a pure function of `ApiFeature.codeGenName`, `ApiRoutes.name` and the route
 * pattern, so the emitted SDK is stable across runs and reviewable as a diff. Nothing consults the
 * Kotlin class names — those are an implementation detail of the server, while the group name is part
 * of the API surface.
 */
internal object TsClientNames {

    /** `funktor-conf` -> `FunktorConf`. Splits on anything that is not a letter or digit. */
    fun pascal(raw: String): String = raw
        .split(NON_ALPHANUMERIC)
        .filter { it.isNotEmpty() }
        .joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }

    /** `funktor-conf` -> `funktorConf`. */
    fun camel(raw: String): String = pascal(raw).replaceFirstChar { it.lowercaseChar() }

    /** `FunktorConf` -> `FunktorConfClient`, the per-feature aggregate. */
    fun clientClass(featureCodeGenName: String): String = "${pascal(featureCodeGenName)}Client"

    /** `FunktorConf` -> `funktorConfClient.ts`. */
    fun clientFile(featureCodeGenName: String): String = "${camel(featureCodeGenName)}Client.ts"

    /** `funktor-conf` -> `FunktorConfApi`, the class carrying one route group's endpoints. */
    fun groupClass(groupName: String): String = "${pascal(groupName)}Api"

    /** `funktor-conf` -> `funktorConf`, the aggregate's member for that group. */
    fun groupMember(groupName: String): String = camel(groupName)

    /**
     * The member name for [route] — `codeGen { funcName = ... }` when set, else derived.
     *
     * The derived form is deliberately verbose (`getApiFunktorConfEventsById`): it is unambiguous, it
     * cannot collide for two different routes of the same group, and being ugly is the point — it
     * makes an unnamed endpoint obvious in review so `funcName` gets set. A pretty guess would be
     * quietly wrong instead.
     */
    fun endpointMember(route: ApiRoute<*>): String {
        route.codeGen.funcName?.let { return it }

        val segments = route.pattern.pattern
            .split('/')
            .filter { it.isNotEmpty() }
            .joinToString("") { segment ->
                when {
                    // `{id}` -> `ById`, so two routes differing only by a path param stay distinct.
                    segment.startsWith("{") -> "By${pascal(segment.trim('{', '}').removeSuffix("..."))}"
                    else -> pascal(segment)
                }
            }

        return route.method.value.lowercase() + segments
    }

    private val NON_ALPHANUMERIC = Regex("[^A-Za-z0-9]+")
}
