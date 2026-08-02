package io.peekandpoke.ultra.codegen.ts

import io.peekandpoke.ultra.codegen.sdk.TsSdkOutput

/**
 * The hand-written TypeScript that generated code is emitted against.
 *
 * These are checked-in resources rather than generated output: they mirror things a type graph cannot
 * describe — the `ApiResponse` envelope's generic shape, the transport contract, the SSE wire format —
 * and they are maintained by hand next to the Kotlin they mirror.
 *
 * Nothing here is emitted automatically. A contributor asks for exactly the modules its output
 * imports, so an SDK never carries runtime it does not use — [Module.DateTime], for instance, ships
 * only when a datetime type was actually reached.
 */
object TsRuntime {

    /**
     * A runtime module, as both a classpath resource and an import specifier.
     *
     * [moduleSpecifier] carries the `.ts` EXTENSION deliberately. Extensionless resolves only under
     * `moduleResolution: bundler`; Node's type stripping and `node16`/`nodenext` both reject it with
     * `ERR_MODULE_NOT_FOUND` while `tsc` stays silent, so the SDK would type-check and then fail to
     * load. With the extension it resolves identically under bundler, node16/nodenext, Node type
     * stripping and Vite — which is also what lets `ts-verify` EXECUTE generated code rather than
     * only type-check it. The cost is `allowImportingTsExtensions` in the consuming tsconfig.
     */
    enum class Module(
        /** Where it is emitted, relative to the SDK root. */
        val path: String,
        /** Where it lives on the classpath. */
        val resource: String,
    ) {
        /** `HttpTransport`, the `fetch` default, and `buildUrl`. */
        Http("runtime/http.ts", "ts/runtime/http.ts"),

        /** The `ApiResponse<T>` envelope, its `apiResponse(schema)` factory, and `Message` / `Insights`. */
        ApiResponse("runtime/apiResponse.ts", "ts/runtime/apiResponse.ts"),

        /** `sseStream` and the `text/event-stream` frame parser. */
        Sse("runtime/sse.ts", "ts/runtime/sse.ts"),

        /** The ultra/datetime types, whose shapes come from custom Slumber codecs. */
        DateTime("runtime/datetime.ts", "ts/runtime/datetime.ts"),

        /** `SdkConfig`, `request`, `unwrap` and the two error types generated clients are built on. */
        Client("runtime/client.ts", "ts/runtime/client.ts"),

        /** `route()` and `RouteRef` — how a generated member carries its own method and uri. */
        Route("runtime/route.ts", "ts/runtime/route.ts"),

        /** `ApiAcl`, the advisory "may this user call this route?" lookup over a fetched matrix. */
        Acl("runtime/acl.ts", "ts/runtime/acl.ts"),

        /** `AuthSession`, the session storage strategies, and the credential-attaching transport. */
        Auth("runtime/auth.ts", "ts/runtime/auth.ts"),

        /** `completeSignIn` — the three-way sign-in outcome, applied to a session. */
        Login("runtime/login.ts", "ts/runtime/login.ts"),

        /** `startAutoRefresh` — refreshes a session before it expires. */
        Refresh("runtime/refresh.ts", "ts/runtime/refresh.ts"),

        /** `AclLoader` — fetches the access matrix and exposes absent/loading/ready. */
        AclLoader("runtime/acl-loader.ts", "ts/runtime/acl-loader.ts");

        /**
         * How generated code imports it, relative to the SDK root.
         *
         * Derived from [path] rather than declared, so the emitted import and the emitted file cannot
         * name different things — and so the `.ts` extension the resolution rule depends on is
         * structural rather than repeated once per module.
         */
        val moduleSpecifier: String get() = "./$path"

        /**
         * Modules this one imports, and which must therefore be emitted alongside it.
         *
         * A computed property rather than a constructor argument because an enum entry cannot
         * reference its siblings during construction.
         */
        val requires: Set<Module>
            get() = when (this) {
                // client.ts imports buildUrl/fetchTransport and the apiResponse factory.
                Client -> setOf(Http, ApiResponse)
                // sse.ts's `stream` takes an SdkConfig and builds the URL, so it needs both. The
                // dependency runs THIS way round on purpose: a client without SSE endpoints must not
                // drag the event-stream parser into the SDK.
                Sse -> setOf(Client, Http)
                // acl.ts borrows RouteRef and nothing else — deliberately NOT Client, so the access
                // lookup does not drag the transport in behind it.
                Acl -> setOf(Route)
                // auth.ts wraps an HttpTransport. Deliberately NOT Client: the session knows nothing
                // about the envelope or about generated members.
                Auth -> setOf(Http)
                // login.ts drives an AuthSession and reads the ApiResponse envelope.
                Login -> setOf(Auth, ApiResponse)
                // refresh.ts imports all three DIRECTLY — applySignIn from login.ts, AuthSession from
                // auth.ts, ApiResponse from apiResponse.ts. Declaring only Login would still emit them
                // through the closure, but `requires` states DIRECT imports and TsRuntimeSpec checks it.
                Refresh -> setOf(Login, Auth, ApiResponse)
                // acl-loader.ts imports ApiAcl/AccessMatrix, AuthSession and the envelope.
                AclLoader -> setOf(Acl, Auth, ApiResponse)
                Http, ApiResponse, DateTime, Route -> emptySet()
            }
    }

    /**
     * Plans [modules] into [out], together with everything they import.
     *
     * The closure over [Module.requires] is not a convenience: a caller asking for [Module.Client]
     * and getting only `client.ts` would produce an SDK that type-checks nowhere and loads nowhere,
     * and the missing file would surface as a module-resolution error inside generated output rather
     * than as anything naming the contributor.
     *
     * Emitted via `out.shared`, not `out.file`: several contributors legitimately need the same
     * module — the auth session runtime imports `runtime/http.ts`, which the REST contributor also
     * ships — and `file` is exclusive, so the second one to ask would be a hard error. Content comes
     * from one resource per module, so it is identical by construction and dedupes; a divergence
     * would still fail, naming both.
     */
    fun emit(out: TsSdkOutput.Scope, modules: Set<Module>) {
        closureOf(modules).forEach { module ->
            out.sharedResource(module.resource, to = module.path)
        }
    }

    /**
     * [modules] plus everything they transitively require.
     *
     * Iterative rather than recursive so a future cycle in [Module.requires] terminates instead of
     * blowing the stack.
     */
    fun closureOf(modules: Set<Module>): Set<Module> {
        val result = linkedSetOf<Module>()
        val pending = ArrayDeque(modules)

        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()

            if (result.add(next)) {
                pending.addAll(next.requires)
            }
        }

        return result
    }
}
