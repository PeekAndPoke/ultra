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

    /** A runtime module, as both a classpath resource and an import specifier. */
    enum class Module(
        /** How generated code imports it, relative to the SDK root. */
        val moduleSpecifier: String,
        /** Where it is emitted, relative to the SDK root. */
        val path: String,
        /** Where it lives on the classpath. */
        val resource: String,
    ) {
        /** `HttpTransport`, the `fetch` default, and `buildUrl`. */
        Http("./runtime/http", "runtime/http.ts", "ts/runtime/http.ts"),

        /** The `ApiResponse<T>` envelope, its `apiResponse(schema)` factory, and `Message` / `Insights`. */
        ApiResponse("./runtime/apiResponse", "runtime/apiResponse.ts", "ts/runtime/apiResponse.ts"),

        /** `sseStream` and the `text/event-stream` frame parser. */
        Sse("./runtime/sse", "runtime/sse.ts", "ts/runtime/sse.ts"),

        /** The ultra/datetime types, whose shapes come from custom Slumber codecs. */
        DateTime("./runtime/datetime", "runtime/datetime.ts", "ts/runtime/datetime.ts"),
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
