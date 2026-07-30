package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.codegen.sdk.TsSdkEmitContext
import io.peekandpoke.ultra.codegen.sdk.TsSdkRoots
import io.peekandpoke.ultra.codegen.ts.TsClientEmitter
import io.peekandpoke.ultra.codegen.ts.TsClientSpec
import io.peekandpoke.ultra.codegen.ts.TsRuntime
import io.peekandpoke.ultra.remote.ApiResponse
import kotlin.reflect.KType

/**
 * Turns the server's REST route graph into TypeScript API clients.
 *
 * The emitted shape MIRRORS THE BACKEND: an [ApiFeature] becomes one client class, each of its
 * [ApiRoutes] groups becomes a class, and each route becomes a member. Nothing is invented, so a
 * frontend developer reading `FunktorConfApi.kt` finds the same names in `funktorConfClient.ts`.
 *
 * This class only WALKS and NAMES. Rendering lives in [TsClientEmitter] on the ultra side, which is
 * where the `ts-verify` toolchain can compile and execute what it produces.
 *
 * @param features the API features to walk. `Lazy` because the route graph is built during app
 *   startup; this mirrors `ValidateRoutesOnAppStarting`.
 * @param include selects which routes reach the SDK. Defaults to all of them. This is the seam
 *   profiles plug into — a profile narrows the ROOT SET, and every consequence (unreached claims,
 *   unshipped runtime modules) then falls out of the existing walk rather than needing a
 *   tree-shaking stage. Reading `CodeGenHints.tags` here is the intended use.
 */
class RestApiTsContributor(
    private val features: Lazy<List<ApiFeature>>,
    private val include: (ApiRoute<*>) -> Boolean = { true },
) : TsSdkContributor {

    companion object {
        const val NAME: String = "funktor:rest"
    }

    override val name: String = NAME

    /** A selected route, before its response type has been resolved to a reference. */
    private data class Selected(
        val member: String,
        val httpMethod: String,
        val pattern: String,
        val responseType: KType,
        /** Unique across the whole run — how the resolved reference is found again at emit time. */
        val rootLabel: String,
        val doc: String?,
        val pathParams: List<TsClientSpec.Param>,
        val queryParams: List<TsClientSpec.Param>,
    )

    private data class SelectedGroup(
        val className: String,
        val member: String,
        val doc: String?,
        val endpoints: List<Selected>,
    )

    private data class SelectedClient(
        val className: String,
        val fileName: String,
        val doc: String?,
        val groups: List<SelectedGroup>,
    )

    /**
     * The selection, computed once and shared by both phases.
     *
     * `contribute` and `emit` MUST agree on the route set — a root that no member renders is dead
     * weight in `models.ts`, and a member whose root was never contributed renders against a type the
     * walk never validated. Deriving both from one lazy value makes disagreement impossible, rather
     * than relying on two traversals staying in step.
     */
    private val selection: List<SelectedClient> by lazy { select() }

    override fun contribute(roots: TsSdkRoots) {
        selection.forEach { client ->
            client.groups.forEach { group ->
                group.endpoints.forEach { endpoint ->
                    // The PAYLOAD is rooted, not the envelope: `ApiResponse<T>` is hand-written in
                    // runtime/apiResponse.ts and no walk should reach it.
                    roots.root(endpoint.responseType, endpoint.rootLabel)
                }
            }
        }
    }

    override fun emit(context: TsSdkEmitContext) {
        if (selection.isEmpty()) return

        TsRuntime.emit(context.out, setOf(TsRuntime.Module.Client))

        val emitter = TsClientEmitter(context.model)

        selection.forEach { client ->
            context.out.file(path = client.fileName, content = emitter.emit(specOf(client, context)))
        }
    }

    /** Builds the render-ready spec, resolving each endpoint's response type via its root label. */
    private fun specOf(client: SelectedClient, context: TsSdkEmitContext) = TsClientSpec(
        className = client.className,
        fileName = client.fileName,
        doc = client.doc,
        groups = client.groups.map { group ->
            TsClientSpec.Group(
                className = group.className,
                member = group.member,
                doc = group.doc,
                endpoints = group.endpoints.map { endpoint ->
                    TsClientSpec.Endpoint(
                        member = endpoint.member,
                        httpMethod = endpoint.httpMethod,
                        pattern = endpoint.pattern,
                        responseRef = context.model.refForRoot(endpoint.rootLabel),
                        doc = endpoint.doc,
                        pathParams = endpoint.pathParams,
                        queryParams = endpoint.queryParams,
                    )
                },
            )
        },
    )

    /** Walks the features, applying [include] and rejecting anything not yet supported. */
    private fun select(): List<SelectedClient> = features.value.mapNotNull { feature ->
        val groups = feature.getRouteGroups().mapNotNull { group ->
            val endpoints = group.all.filter(include).map { route -> selectedOf(feature, group, route) }

            val duplicates = endpoints.groupBy { it.member }.filterValues { it.size > 1 }.keys

            check(duplicates.isEmpty()) {
                "Route group '${group.name}' of feature '${feature.codeGenName}' produces the same " +
                        "TypeScript member name twice: ${duplicates.joinToString()}. Two routes cannot " +
                        "share a member. Fix: give one of them a distinct `codeGen { funcName = ... }`."
            }

            endpoints.takeIf { it.isNotEmpty() }?.let {
                SelectedGroup(
                    className = TsClientNames.groupClass(group.name),
                    member = TsClientNames.groupMember(group.name),
                    doc = "Routes of the `${group.name}` group.",
                    endpoints = it,
                )
            }
        }

        // A feature all of whose routes were filtered out emits no file at all, rather than an empty
        // class — the profile seam is meant to remove things completely.
        groups.takeIf { it.isNotEmpty() }?.let {
            SelectedClient(
                className = TsClientNames.clientClass(feature.codeGenName),
                fileName = TsClientNames.clientFile(feature.codeGenName),
                doc = feature.description.takeIf { d -> d.isNotBlank() },
                groups = it,
            )
        }
    }

    private fun selectedOf(feature: ApiFeature, group: ApiRoutes, route: ApiRoute<*>): Selected {
        val member = TsClientNames.endpointMember(route)

        // `WithBody`, `WithBodyAndParams` and `Sse` are not implemented yet. They are REJECTED rather
        // than skipped: silently emitting a client that is missing half its endpoints is the
        // wrong-and-quiet failure this whole module exists to remove, and a frontend would only
        // notice at the call site.
        check(route is ApiRoute.Plain<*> || route is ApiRoute.WithParams<*, *>) {
            "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                    "${group.name} / $member) is an ${route::class.simpleName} route, which the " +
                    "TypeScript generator does not support yet — only routes without a request body " +
                    "are implemented. See .claude/tasks/20260730-funktor-codegen-rest-contributor.md."
        }

        val (pathParams, queryParams) = paramsOf(route, feature, group, member)

        return Selected(
            member = member,
            httpMethod = route.method.value,
            pattern = route.pattern.pattern,
            responseType = payloadTypeOf(route, feature, group, member),
            // Qualified so two features, or two groups, cannot collide — the builder rejects that
            // anyway, but with a message about labels rather than about routes.
            rootLabel = "$NAME:${feature.codeGenName}:${group.name}:$member",
            doc = route.docs.name,
            pathParams = pathParams,
            queryParams = queryParams,
        )
    }

    /**
     * Splits [route]'s PARAMS into path and query parameters, refusing any type that cannot be mapped.
     *
     * The split is the route PATTERN's: a constructor property whose name appears as a `{placeholder}`
     * fills that placeholder, everything else becomes a query parameter. This is exactly what
     * `TypedRouteRenderer` does (`funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:44`),
     * and `buildUrl` is already written against the same rule — so neither side reinvents it.
     */
    private fun paramsOf(
        route: ApiRoute<*>,
        feature: ApiFeature,
        group: ApiRoutes,
        member: String,
    ): Pair<List<TsClientSpec.Param>, List<TsClientSpec.Param>> {
        val typed = route.typedRoute
        val placeholders = typed.parsedUriParams.toSet()

        val params = typed.reifiedParamsType.ctorParams2Types.map { (parameter, type) ->
            val name = parameter.name ?: error(
                "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                        "${group.name} / $member) has an unnamed parameter, so no TypeScript member " +
                        "signature can be built for it."
            )

            val tsType = UrlParamTypes.of(type) ?: error(
                "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                        "${group.name} / $member) parameter '$name' has type '$type', which the " +
                        "generator cannot map to a URL parameter type. URL parameters travel as text, " +
                        "so only types whose wire form is provable are mapped — String, the numeric " +
                        "types, Boolean, enums, and value classes over those. Fix: change the " +
                        "parameter type, or claim '$type' as a URL parameter type once that " +
                        "mechanism exists."
            )

            // Optional in TypeScript iff the Kotlin constructor parameter has a default — the same
            // rule the model emitter applies to object properties.
            TsClientSpec.Param(name = name, tsType = tsType, optional = parameter.isOptional)
        }

        val unfilled = placeholders - params.map { it.name }.toSet()

        check(unfilled.isEmpty()) {
            "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                    "${group.name} / $member) has placeholders ${unfilled.joinToString()} with no " +
                    "matching parameter, so the generated call could never fill them. `buildUrl` " +
                    "throws on an unfilled placeholder, which would surface as a puzzling 404."
        }

        return params.partition { it.name in placeholders }
    }

    /**
     * The PAYLOAD type of [route], unwrapped from its `ApiResponse<T>` envelope.
     *
     * A route's `responseType` is the ENVELOPE — a handler returns `ApiResponse.ok(payload)`, so
     * `RESPONSE` binds to `ApiResponse<List<Talk>>`, not to `List<Talk>`. Rooting it unwrapped would
     * walk the envelope into `models.ts`, competing with the hand-written `runtime/apiResponse.ts`,
     * and the generated call would wrap it a SECOND time — every parse would then fail against
     * output the server never produces.
     */
    private fun payloadTypeOf(route: ApiRoute<*>, feature: ApiFeature, group: ApiRoutes, member: String): KType {
        val declared = route.responseType.type

        check(declared.classifier == ApiResponse::class) {
            "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                    "${group.name} / $member) declares the response type '$declared', which is not an " +
                    "ApiResponse envelope. Every funktor endpoint answers with one, so this is either a " +
                    "route built outside the normal `mount` path or a generator bug."
        }

        return declared.arguments.firstOrNull()?.type ?: error(
            "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                    "${group.name} / $member) has a star-projected response envelope, so the payload " +
                    "type is not knowable. Give the endpoint a concrete response type."
        )
    }
}
