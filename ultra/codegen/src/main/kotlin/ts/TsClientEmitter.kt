package io.peekandpoke.ultra.codegen.ts

import io.peekandpoke.ultra.codegen.model.TsTypeRef
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.printer.CodePrinter

/**
 * A client to emit, described without reference to where its endpoints came from.
 *
 * Deliberately framework-neutral: the funktor REST contributor builds one of these from an
 * `ApiFeature`, but so could a contributor over any other route description. Keeping the SHAPE here
 * and the WALK there is also what puts the emitted TypeScript under `ts-verify`, which lives in this
 * module — a renderer that could only be exercised from `funktor/codegen` would need a second copy of
 * the whole toolchain to prove anything.
 */
data class TsClientSpec(
    /** The aggregate class, e.g. `FunktorConfClient`. */
    val className: String,
    /** Where it is emitted, relative to the SDK root, e.g. `funktorConfClient.ts`. */
    val fileName: String,
    /** TSDoc for the aggregate, one line. */
    val doc: String?,
    val groups: List<Group>,
) {
    /** One group of endpoints — an `ApiRoutes` group on the funktor side. */
    data class Group(
        /** The class carrying the endpoints, e.g. `FunktorConfApi`. */
        val className: String,
        /** The aggregate's member for this group, e.g. `funktorConf`. */
        val member: String,
        val doc: String?,
        val endpoints: List<Endpoint>,
    )

    /** One endpoint. */
    data class Endpoint(
        /** The member name, e.g. `listEvents`. */
        val member: String,
        /** Uppercase HTTP method, e.g. `GET`. */
        val httpMethod: String,
        /** The route pattern with `{name}` placeholders, passed through to `buildUrl`. */
        val pattern: String,
        /**
         * The RESPONSE PAYLOAD's reference — not the envelope's. `null` exactly when [stream].
         *
         * `ApiResponse<T>` is hand-written in `runtime/apiResponse.ts` and no walk reaches it; the
         * generated call wraps this payload schema at run time.
         */
        val responseRef: TsTypeRef?,
        val doc: String?,
        /** Parameters filling `{name}` placeholders in [pattern]. */
        val pathParams: List<Param> = emptyList(),
        /** Parameters appended to the query string. */
        val queryParams: List<Param> = emptyList(),
        /**
         * The REQUEST BODY's reference, or `null` for a bodiless route.
         *
         * Used in TYPE position — the caller passes a value, so the member takes `SaveTalkRequest`
         * rather than its schema. `request` JSON-encodes it; nothing validates it client-side, which
         * is correct: the server is the authority on what it accepts.
         */
        val bodyRef: TsTypeRef? = null,
        /**
         * True for a server-sent-events endpoint.
         *
         * Such a member returns `AsyncGenerator<SseEvent>` and has NO response schema: `ApiRoute.Sse`
         * declares `responseType: TypeRef<Unit>`, so the stream's payload type is not on the route at
         * all. Events carry raw `data` strings and the caller parses them. Typing them would mean
         * changing how SSE endpoints are declared server-side — a decision taken deliberately, not an
         * omission (maintainer, 2026-07-30).
         */
        val stream: Boolean = false,
        /**
         * True when the server's auth rules admit an ANONYMOUS caller.
         *
         * Whoever builds the spec owns this: it comes from evaluating the route's real rule chain,
         * which is a funktor concept this module deliberately knows nothing about. Defaults to
         * `false` — a contributor that cannot determine publicness must not claim it.
         */
        val isPublic: Boolean = false,
    ) {
        init {
            require((responseRef == null) == stream) {
                "An endpoint has a response schema iff it is not a stream: '$member' has " +
                        "responseRef=${responseRef != null} and stream=$stream."
            }

            require(!(stream && bodyRef != null)) {
                "'$member' is a stream with a request body. ApiRoute.Sse routes are GET and carry none."
            }

            // `runtime/route.ts` types HttpMethod as a CLOSED union so `member.method` is worth
            // reading. Refusing here turns an unlisted method into a named generation error, instead
            // of TypeScript that fails to compile inside output the consuming app cannot edit.
            require(httpMethod in KNOWN_HTTP_METHODS) {
                "Endpoint '$member' uses HTTP method '$httpMethod', which `HttpMethod` in " +
                        "runtime/route.ts does not list. Widen that union and this check together."
            }
        }

        /** Every parameter, in one object as the caller sees it. */
        val allParams: List<Param> get() = pathParams + queryParams

        companion object {
            /** Must stay in step with `HttpMethod` in `ts/runtime/route.ts`. */
            val KNOWN_HTTP_METHODS: Set<String> =
                setOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
        }
    }

    /**
     * One URL parameter.
     *
     * [tsType] is the type ON THE WIRE, already rendered — a URL parameter is not JSON, so it is not
     * a [TsTypeRef]. Whoever builds the spec owns that mapping, because it depends on the server's
     * parameter converters rather than on TypeScript.
     */
    data class Param(
        val name: String,
        val tsType: String,
        /** True when the Kotlin constructor parameter has a default, so the caller may omit it. */
        val optional: Boolean,
        /**
         * How the value must be formatted, e.g. `ISO-8601 instant`.
         *
         * Emitted as `@param`. [tsType] is usually `string`, which says nothing about what the server
         * can parse back — a caller passing the wrong format gets a 400, and this is the only place
         * to warn them before they do.
         */
        val format: String? = null,
    )
}

/**
 * Renders a [TsClientSpec] as TypeScript.
 *
 * Two shape decisions are load-bearing and were settled by running the pinned compiler, not by
 * reasoning:
 *
 * - **Members are arrow-function class fields, never prototype methods.** A prototype method
 *   type-checks when destructured and throws at run time; `const { getEvent } = client.conf` is the
 *   Vue-composable idiom, so that is wrong-and-quiet. Class fields are standard JS, so they also
 *   survive `erasableSyntaxOnly` and Node's type stripping.
 * - **The constructor declares a field and assigns it.** A parameter property
 *   (`constructor(private readonly config: SdkConfig)`) is **TS1294** under `erasableSyntaxOnly`.
 */
class TsClientEmitter(private val model: TypeModel) {

    private val renderer = TsRenderer(model)

    /** Renders [spec] as a complete TypeScript module. */
    fun emit(spec: TsClientSpec): String {
        val endpoints = spec.groups.flatMap { it.endpoints }

        // Bodies count too: their type names are referenced in the member signature, so a client with
        // a body type it never imports does not compile.
        val referenced = endpoints
            .flatMap {
                (it.responseRef?.referencedIds() ?: emptyList()) +
                        (it.bodyRef?.referencedIds() ?: emptyList())
            }
            .distinct()

        // A claimed type is NOT exported by models.ts — it is imported into it from the module that
        // owns it, so a client referencing one must import it from that module too. Getting this
        // wrong emits a name models.ts never exported, which only `tsc` would catch.
        val (claimed, declared) = referenced.partition { id ->
            model.usedClaims.containsKey(id.cls.qualifiedName)
        }

        val modelNames = declared.map { renderer.nameOf(it) }.distinct().sorted()

        val claimImports = claimed
            .mapNotNull { model.usedClaims[it.cls.qualifiedName] }
            .filter { !it.opaque && it.importFrom != null }
            .groupBy { it.importFrom!! }
            .toSortedMap()

        return CodePrinter.print {
            appendLine("// Generated by ultra:codegen. Do not edit — regenerate with `sdk:ts:generate`.")
            nl()

            // `z.` only appears for containers (arrays, records, nullables), so a client returning
            // only declared objects must not carry an unused zod import.
            if (endpoints.any { it.responseRef != null && renderer.schema(it.responseRef).contains("z.") }) {
                appendLine("import { z } from 'zod'")
            }

            // `request` only when a non-stream endpoint exists: a stream-only client would otherwise
            // carry a dead import, which fails a consuming app compiled with `noUnusedLocals` — and
            // that app cannot edit the file.
            val clientImports = buildList {
                add("type SdkConfig")
                if (endpoints.any { !it.stream }) {
                    add("type CallOptions")
                    add("request")
                }
            }

            appendLine("import { ${clientImports.joinToString(", ")} } from ${tsStringLiteral(CLIENT_MODULE)}")

            // Only the wrappers actually used: a consuming app compiled with `noUnusedLocals` cannot
            // edit this file, so a dead import would break a build nobody can fix.
            val routeImports = buildList {
                if (endpoints.any { it.isPublic }) add("publicRoute")
                if (endpoints.any { !it.isPublic }) add("route")
            }

            appendLine("import { ${routeImports.joinToString(", ")} } from ${tsStringLiteral(ROUTE_MODULE)}")

            // Only when a stream endpoint exists: an SDK without SSE must not carry the event-stream
            // parser, which the runtime dependency closure would otherwise pull in.
            if (endpoints.any { it.stream }) {
                appendLine(
                    "import { type SseEvent, type SseOptions, stream } from " +
                            tsStringLiteral(SSE_MODULE)
                )
            }

            if (modelNames.isNotEmpty()) {
                appendLine("import { ${modelNames.joinToString(", ")} } from ${tsStringLiteral(MODELS_MODULE)}")
            }

            claimImports.forEach { (module, claims) ->
                val names = claims.flatMap { listOfNotNull(it.tsName, it.schema) }.distinct().sorted()
                appendLine("import { ${names.joinToString(", ")} } from ${tsStringLiteral(module)}")
            }

            spec.groups.forEach { group ->
                nl()
                appendGroup(group)
            }

            nl()
            appendAggregate(spec)
        }
    }

    private fun CodePrinter.appendGroup(group: TsClientSpec.Group) {
        group.doc?.let { appendLine("/** ${it.oneLine()} */") }

        appendLine("export class ${group.className} {")

        indentedRaw {
            appendLine("private readonly config: SdkConfig")
            nl()
            appendLine("constructor(config: SdkConfig) {")
            indentedRaw { appendLine("this.config = config") }
            appendLine("}")

            group.endpoints.forEach { endpoint ->
                nl()
                appendEndpointDoc(endpoint)
                appendEndpoint(endpoint)
            }
        }

        appendLine("}")
    }

    /**
     * The member's TSDoc: its description, plus a `@param` line for every parameter that declares a
     * format.
     *
     * `@param` rather than a doc comment on the object-type member, so the signature stays on one
     * line — and IDEs surface both the same way in the call tooltip.
     */
    private fun CodePrinter.appendEndpointDoc(endpoint: TsClientSpec.Endpoint) {
        val formatted = endpoint.allParams.filter { it.format != null }

        when {
            endpoint.doc == null && formatted.isEmpty() -> return

            formatted.isEmpty() -> appendLine("/** ${endpoint.doc!!.oneLine()} */")

            else -> {
                appendLine("/**")
                endpoint.doc?.let { appendLine(" * ${it.oneLine()}"); appendLine(" *") }
                formatted.forEach { appendLine(" * @param params.${it.name} ${it.format!!.oneLine()}") }
                appendLine(" */")
            }
        }
    }

    /**
     * One endpoint member.
     *
     * Path and query parameters are taken as ONE object, mirroring the Kotlin PARAMS class the caller
     * would fill in — which side of the URL each lands on is the route pattern's business, not the
     * caller's — and split back apart in the call. Their names cannot collide: they are properties of
     * a single class.
     */
    private fun CodePrinter.appendEndpoint(endpoint: TsClientSpec.Endpoint) {
        // Rendered HERE from the endpoint's own reference, never looked up by name. Keying schemas by
        // member name silently gave one group another's schema whenever two groups of the same
        // feature shared a member — `FunktorClusterApiFeature` has three groups declaring
        // `funcName = "list"`. `z.array(X)` type-checks for any X, so tsc stayed silent and every
        // call threw ApiProtocolError against a perfectly fresh SDK.
        val schema = endpoint.responseRef?.let { renderer.schema(it) }

        val params = endpoint.allParams

        // `(params, body)`, mirroring the Kotlin endpoint's own argument order. `params` stays
        // REQUIRED whenever the route has any, even when every member is optional — one rule with no
        // special cases beats a signature whose argument order flips based on optionality.
        val arguments = buildList {
            if (params.isNotEmpty()) {
                add(
                    params.joinToString(separator = "; ", prefix = "params: { ", postfix = " }") {
                        "${it.name}${if (it.optional) "?" else ""}: ${it.tsType}"
                    }
                )
            }

            endpoint.bodyRef?.let { add("body: ${renderer.type(it)}") }

            // Trailing and optional on BOTH kinds. For a stream these are headers/signal/fetchImpl,
            // and auth cannot be inherited from a transport wrapper because the stream does not use
            // one. For a request it is the AbortSignal — `client.ts` documents `signal` as the way to
            // cancel on unmount, and until this was added no generated member could actually pass one,
            // so a Vue component had no way to abort.
            add(if (endpoint.stream) "options?: SseOptions" else "options?: CallOptions")
        }

        val returns = if (endpoint.stream) ": AsyncGenerator<SseEvent>" else ""

        // Every member — streams included — is wrapped so it carries its own method and uri. That
        // pair is the key `UserApiAccessMatrix` is indexed by, so `acl.canAccess(api.getEvent)` works
        // without the caller repeating a string the generator already knows. Purely additive:
        // `route()` returns `F & RouteRef`, so existing call sites are untouched.
        // A distinct function rather than a flag argument — the name states the fact at the call
        // site, where a trailing boolean after a multi-line arrow would be easy to miss.
        val wrapper = if (endpoint.isPublic) "publicRoute" else "route"

        appendLine(
            "readonly ${endpoint.member} = $wrapper(${tsStringLiteral(endpoint.httpMethod)}, " +
                    "${tsStringLiteral(endpoint.pattern)}, (${arguments.joinToString(", ")})$returns =>"
        )

        indentedRaw {
            // `stream` and `request` deliberately mirror each other: same config, same pattern, same
            // path/query object. Only the payload differs, because a stream has no envelope.
            val call = when {
                endpoint.stream ->
                    "stream(this.config, ${tsStringLiteral(endpoint.pattern)}"

                else ->
                    "request(this.config, ${tsStringLiteral(endpoint.httpMethod)}, " +
                            "${tsStringLiteral(endpoint.pattern)}, $schema"
            }

            // The trailing `)` closes `route(`, opened above.
            if (params.isEmpty() && endpoint.bodyRef == null) {
                appendLine(if (endpoint.stream) "$call, {}, options))" else "$call, { ...options }))")
                return@indentedRaw
            }

            appendLine("$call, {")

            indentedRaw {
                listOf("path" to endpoint.pathParams, "query" to endpoint.queryParams)
                    .filter { (_, group) -> group.isNotEmpty() }
                    .forEach { (key, group) ->
                        appendLine(
                            group.joinToString(", ", "$key: { ", " },") { "${it.name}: params.${it.name}" }
                        )
                    }

                if (endpoint.bodyRef != null) {
                    appendLine("body,")
                }

                // Spread last. This is DEFENCE IN DEPTH, not a current guarantee: `CallOptions`
                // carries only `signal` today, so the order is unobservable and a mutant that moves
                // it survives — correctly. It matters the moment `CallOptions` gains a key the
                // generated object also sets, and putting it last now is free.
                if (!endpoint.stream) {
                    appendLine("...options,")
                }
            }

            // The trailing `)` closes `route(`, opened above.
            appendLine(if (endpoint.stream) "}, options))" else "}))")
        }
    }

    private fun CodePrinter.appendAggregate(spec: TsClientSpec) {
        spec.doc?.let { appendLine("/** ${it.oneLine()} */") }

        appendLine("export class ${spec.className} {")

        indentedRaw {
            spec.groups.forEach { group ->
                appendLine("readonly ${group.member}: ${group.className}")
            }

            nl()
            appendLine("constructor(config: SdkConfig) {")
            indentedRaw {
                spec.groups.forEach { group ->
                    appendLine("this.${group.member} = new ${group.className}(config)")
                }
            }
            appendLine("}")
        }

        appendLine("}")
    }

    /** Collapses [this] to a single line, so a multi-line description cannot break out of `/** */`. */
    private fun String.oneLine(): String = trim()
        .lineSequence()
        .joinToString(" ") { it.trim() }
        .replace("*/", "* /")

    companion object {
        private val CLIENT_MODULE: String = TsRuntime.Module.Client.moduleSpecifier

        private val SSE_MODULE: String = TsRuntime.Module.Sse.moduleSpecifier

        private val ROUTE_MODULE: String = TsRuntime.Module.Route.moduleSpecifier

        /** Where the type declarations live. Emitted by `TsSdkBuilder`, at the SDK root. */
        private const val MODELS_MODULE: String = "./models.ts"
    }
}
