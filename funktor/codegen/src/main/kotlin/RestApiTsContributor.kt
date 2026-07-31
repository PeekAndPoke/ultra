package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.codegen.model.TsUrlParamClaims
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.codegen.sdk.TsSdkEmitContext
import io.peekandpoke.ultra.codegen.sdk.TsSdkRoots
import io.peekandpoke.ultra.codegen.ts.TsClientEmitter
import io.peekandpoke.ultra.codegen.ts.TsClientSpec
import io.peekandpoke.ultra.codegen.ts.TsRuntime
import io.peekandpoke.ultra.codegen.ts.isBareIdentifier
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
        /** `null` for a stream: `ApiRoute.Sse` carries no payload type. */
        val responseType: KType?,
        /** Unique across the whole run — how the resolved reference is found again at emit time. */
        val rootLabel: String,
        val doc: String?,
        val pathParams: List<TsClientSpec.Param>,
        val queryParams: List<TsClientSpec.Param>,
        /** The request body's type, or `null` for a bodiless route. */
        val bodyType: KType?,
        /** Root label for [bodyType]; `null` exactly when [bodyType] is. */
        val bodyRootLabel: String?,
        val stream: Boolean,
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
     * `contribute` and `emit` MUST agree on the route set — a root that no member renders is dead
     * weight in `models.ts`, and a member whose root was never contributed renders against a type the
     * walk never validated. Computing it once and reading it twice makes disagreement impossible,
     * rather than relying on two traversals staying in step.
     */
    private var selected: List<SelectedClient>? = null

    /**
     * The selection, computed in [contribute] and reused by [emit].
     *
     * Not a plain `lazy` because it needs the URL-parameter claims, which only arrive with the
     * contribute phase. `TsSdkBuilder` always runs [contribute] before [emit], so this is set by then.
     */
    private val selection: List<SelectedClient>
        get() = selected ?: error(
            "RestApiTsContributor.emit ran without contribute. The phases are ordered by TsSdkBuilder, " +
                    "so this means the contributor was driven by something else."
        )

    override fun contribute(roots: TsSdkRoots) {
        selected = select(roots.urlParamClaims)

        selection.forEach { client ->
            client.groups.forEach { group ->
                group.endpoints.forEach { endpoint ->
                    // The PAYLOAD is rooted, not the envelope: `ApiResponse<T>` is hand-written in
                    // runtime/apiResponse.ts and no walk should reach it.
                    endpoint.responseType?.let { roots.root(it, endpoint.rootLabel) }

                    // The body is a second root. It must be WALKED, not merely named: a request type
                    // reachable from nowhere else would otherwise never be declared in models.ts, and
                    // the client would reference a type that does not exist.
                    endpoint.bodyType?.let { roots.root(it, endpoint.bodyRootLabel!!) }
                }
            }
        }
    }

    override fun emit(context: TsSdkEmitContext) {
        if (selection.isEmpty()) return

        // The SSE runtime ships only when a stream endpoint exists — an SDK with no streams must not
        // carry the event-stream parser. `TsRuntime.emit` closes over each module's requirements, so
        // asking for Sse also brings Client and Http.
        val runtime = buildSet {
            add(TsRuntime.Module.Client)

            if (selection.any { c -> c.groups.any { g -> g.endpoints.any { it.stream } } }) {
                add(TsRuntime.Module.Sse)
            }
        }

        TsRuntime.emit(context.out, runtime)

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
                        responseRef = endpoint.responseType?.let { context.model.refForRoot(endpoint.rootLabel) },
                        doc = endpoint.doc,
                        pathParams = endpoint.pathParams,
                        queryParams = endpoint.queryParams,
                        bodyRef = endpoint.bodyRootLabel?.let { context.model.refForRoot(it) },
                        stream = endpoint.stream,
                    )
                },
            )
        },
    )

    /** Walks the features, applying [include] and rejecting anything not yet supported. */
    private fun select(urlParams: TsUrlParamClaims): List<SelectedClient> = features.value.mapNotNull { feature ->
        // MERGED BY GROUP NAME, not one class per ApiRoutes instance. Two groups may legitimately
        // share a name — `funktor:auth` declares `ApiRoutes("login")` twice, once `public()` and once
        // `authenticated()` — and they are one logical group to a client. Emitting a class per
        // instance produced two `LoginApi` classes and two `login` members in one file, which only
        // `tsc` caught (TS2300, found by generating the demo's real API).
        val groups = feature.getRouteGroups()
            .groupBy { it.name }
            .mapNotNull { (groupName, instances) ->
                val endpoints = instances.flatMap { group ->
                    group.all.filter(include).map { route -> selectedOf(feature, group, route, urlParams) }
                }

                // Runs across the MERGED set, so a collision between the two halves is caught too.
                val duplicates = endpoints.groupBy { it.member }.filterValues { it.size > 1 }.keys

                check(duplicates.isEmpty()) {
                    "Route group '$groupName' of feature '${feature.codeGenName}' produces the same " +
                            "TypeScript member name twice: ${duplicates.joinToString()}. Two routes " +
                            "cannot share a member. Fix: give one of them a distinct " +
                            "`codeGen { funcName = ... }`."
                }

                endpoints.takeIf { it.isNotEmpty() }?.let {
                    SelectedGroup(
                        className = TsClientNames.groupClass(groupName),
                        member = TsClientNames.groupMember(groupName),
                        doc = "Routes of the `$groupName` group.",
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

    private fun selectedOf(
        feature: ApiFeature,
        group: ApiRoutes,
        route: ApiRoute<*>,
        urlParams: TsUrlParamClaims,
    ): Selected {
        val member = TsClientNames.endpointMember(route)

        val stream = route is ApiRoute.Sse<*>

        val (pathParams, queryParams) = paramsOf(route, feature, group, member, urlParams)

        // Reached through the concrete variants because `bodyType` is not on the ApiRoute base class.
        val bodyType: KType? = when (route) {
            is ApiRoute.WithBody<*, *> -> route.bodyType.type
            is ApiRoute.WithBodyAndParams<*, *, *> -> route.bodyType.type
            else -> null
        }

        return Selected(
            member = member,
            httpMethod = route.method.value,
            pattern = route.pattern.pattern,
            // A stream has no payload type to unwrap: `ApiRoute.Sse.responseType` is TypeRef<Unit>,
            // so events are delivered as raw `data` strings (maintainer decision, 2026-07-30).
            responseType = if (stream) null else payloadTypeOf(route, feature, group, member),
            // Qualified so two features, or two groups, cannot collide — the builder rejects that
            // anyway, but with a message about labels rather than about routes.
            rootLabel = "$NAME:${feature.codeGenName}:${group.name}:$member",
            doc = route.docs.name,
            pathParams = pathParams,
            queryParams = queryParams,
            bodyType = bodyType,
            bodyRootLabel = bodyType?.let { "$NAME:${feature.codeGenName}:${group.name}:$member:body" },
            stream = stream,
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
        urlParams: TsUrlParamClaims,
    ): Pair<List<TsClientSpec.Param>, List<TsClientSpec.Param>> {
        val typed = route.typedRoute
        val placeholders = typed.parsedUriParams.toSet()

        val params = typed.reifiedParamsType.ctorParams2Types.map { (parameter, type) ->
            val name = parameter.name ?: error(
                "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                        "${group.name} / $member) has an unnamed parameter, so no TypeScript member " +
                        "signature can be built for it."
            )

            // A parameter name is emitted both as an object-type member and as `params.<name>`, so
            // it must be a bare identifier. Kotlin permits backticked property names — `val \`a b\`` —
            // which would emit `params.a b` and fail to parse.
            check(isBareIdentifier(name)) {
                "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                        "${group.name} / $member) has parameter '$name', which is not a valid " +
                        "TypeScript identifier. Parameter names are emitted in identifier position, " +
                        "where nothing can be escaped."
            }

            val mapped = UrlParamTypes.of(type, urlParams) ?: error(
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
            // A PATH parameter is never optional, whatever its Kotlin default: omitting it makes
            // `buildUrl` substitute the empty string, which REPLACES the placeholder, so the
            // unfilled-placeholder guard never fires and the client silently requests `/api/x/`.
            // The Kotlin default is unreachable from TypeScript anyway — it only applies server-side.
            TsClientSpec.Param(
                name = name,
                tsType = mapped.tsType,
                optional = parameter.isOptional && name !in placeholders,
                format = mapped.format,
            )
        }

        // A ktor tailcard renders as `{name...}` in the pattern, but `parsedUriParams` strips the
        // suffix — so the name matches, the check below passes, and `buildUrl` then fails to replace
        // `{name...}` and throws on EVERY call. Refuse at generation time instead.
        val tailcards = typed.pattern.pattern
            .let { Regex("\\{([^}]*)\\.\\.\\.}").findAll(it).map { m -> m.groupValues[1] }.toList() }

        check(tailcards.isEmpty()) {
            "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                    "${group.name} / $member) uses ktor tailcard placeholders " +
                    "${tailcards.joinToString { name -> "{$name...}" }}, which the generator does not " +
                    "support: `buildUrl` fills simple `{name}` placeholders only, so the emitted " +
                    "client would throw on every call."
        }

        val unfilled = placeholders - params.map { it.name }.toSet()

        // `{csrf}` is a funktor placeholder filled from CsrfProtection by the server-side renderer,
        // not by a PARAMS property — so it always lands in `unfilled` and the generic message below
        // would blame a missing parameter. Name it for what it is.
        check(!unfilled.contains("csrf")) {
            "Route '${route.method.value} ${route.pattern.pattern}' (${feature.codeGenName} / " +
                    "${group.name} / $member) has a {csrf} placeholder. CSRF tokens are issued by the " +
                    "server (`CsrfProtection`), and the generated client has no way to obtain one, so " +
                    "this route cannot be part of the SDK yet."
        }

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
