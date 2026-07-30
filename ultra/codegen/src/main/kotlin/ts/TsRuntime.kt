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
        DateTime("runtime/datetime.ts", "ts/runtime/datetime.ts");

        /**
         * How generated code imports it, relative to the SDK root.
         *
         * Derived from [path] rather than declared, so the emitted import and the emitted file cannot
         * name different things — and so the `.ts` extension the resolution rule depends on is
         * structural rather than repeated once per module.
         */
        val moduleSpecifier: String get() = "./$path"
    }

    /**
     * Plans [modules] into [out].
     *
     * Emitting the same module from two contributors is a hard error from [TsSdkOutput], so a
     * generator that needs a shared module must be the single one asking for it.
     */
    fun emit(out: TsSdkOutput.Scope, modules: Set<Module>) {
        modules.forEach { module ->
            out.resource(module.resource, to = module.path)
        }
    }
}
