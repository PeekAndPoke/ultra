package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.codegen.sdk.TsSdkOutput

class TsRuntimeSpec : FreeSpec() {

    init {
        "every module resolves to a resource that is actually on the classpath" - {
            TsRuntime.Module.entries.forEach { module ->
                "${module.name}" {
                    val content = this::class.java.classLoader.getResourceAsStream(module.resource)
                        ?.bufferedReader()?.readText()

                    withClue("${module.resource} must be under src/main/resources") {
                        (content != null) shouldBe true
                    }

                    withClue("a runtime module is only useful if it exports something") {
                        content!! shouldContain "export"
                    }
                }
            }
        }

        "emit plans exactly the requested modules" {
            val out = TsSdkOutput()

            TsRuntime.emit(out.scopeFor("test"), setOf(TsRuntime.Module.Http, TsRuntime.Module.Sse))

            out.entries().map { it.path } shouldContainExactlyInAnyOrder listOf("runtime/http.ts", "runtime/sse.ts")
        }

        "emit copies the real resource content, not a placeholder" {
            val out = TsSdkOutput()

            TsRuntime.emit(out.scopeFor("test"), setOf(TsRuntime.Module.ApiResponse))

            out.entries().single().content shouldContain "export function apiResponse"
        }

        "the import specifier and the emitted path agree" - {
            // Generated code imports `./runtime/http.ts`; the file must land at `runtime/http.ts`, or
            // every generated import is a dead link that only `tsc` would catch.
            TsRuntime.Module.entries.forEach { module ->
                "${module.name}" {
                    module.moduleSpecifier shouldBe "./" + module.path
                }
            }
        }

        "the import specifier keeps the .ts extension" - {
            // Asserted SEPARATELY from the agreement above, which held just as well when both sides
            // were extensionless — that is exactly how this shipped. An extensionless specifier
            // resolves under `moduleResolution: bundler` and NOWHERE else: Node's type stripping and
            // node16/nodenext answer ERR_MODULE_NOT_FOUND while `tsc` stays silent, so the SDK
            // type-checks and then fails to load. Measured 2026-07-30, both forms, one compiler.
            TsRuntime.Module.entries.forEach { module ->
                "${module.name}" {
                    withClue("an extensionless specifier only resolves under bundler resolution") {
                        module.moduleSpecifier.endsWith(".ts") shouldBe true
                    }
                }
            }
        }
    }
}
