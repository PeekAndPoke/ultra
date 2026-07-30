package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.ultra.codegen.ts.isBareIdentifier

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
        route.codeGen.funcName?.let { declared ->
            // `funcName` is an unconstrained String that lands in IDENTIFIER position, where no
            // escaping exists. Verified 2026-07-30 against the pinned compiler: a value containing a
            // newline emits a well-formed EXTRA class field that runs on construction, and `tsc`
            // exits 0 — so a route definition could inject arbitrary JavaScript into a file that
            // ships to every user's browser. Refuse, the same way an unmappable parameter type is
            // refused.
            check(isBareIdentifier(declared)) {
                "Route '${route.method.value} ${route.pattern.pattern}' declares " +
                        "codeGen { funcName = \"$declared\" }, which is not a valid TypeScript " +
                        "identifier. It is emitted in identifier position, where nothing can be " +
                        "escaped, so it must match [A-Za-z_$][A-Za-z0-9_$]* exactly."
            }

            return declared
        }

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
