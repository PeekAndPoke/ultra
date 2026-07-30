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

        "emit also plans what the requested modules import" {
            val out = TsSdkOutput()

            TsRuntime.emit(out.scopeFor("test"), setOf(TsRuntime.Module.Client))

            // Asking for the client alone must not produce an SDK whose client.ts imports two files
            // that were never emitted — that surfaces as a module-resolution error inside generated
            // output rather than as anything naming the contributor.
            out.entries().map { it.path } shouldContainExactlyInAnyOrder listOf(
                "runtime/client.ts",
                "runtime/http.ts",
                "runtime/apiResponse.ts",
            )
        }

        "every declared requirement matches what the module actually imports" - {
            // The closure is only as good as `requires`, and `requires` is hand-maintained. This reads
            // the real resource and fails when a module imports a sibling it does not declare — the
            // way that list rots is by someone adding an import, not by editing the list.
            TsRuntime.Module.entries.forEach { module ->
                "${module.name}" {
                    val content = this::class.java.classLoader.getResourceAsStream(module.resource)
                        ?.bufferedReader()?.readText()
                        ?: error("runtime resource '${module.resource}' is not on the classpath")

                    // Runtime modules all live in one directory, so a sibling is imported as
                    // `./<filename>` — the resource is written by hand and cannot use moduleSpecifier.
                    val imported = TsRuntime.Module.entries.filter { other ->
                        other != module && content.contains("from './${other.path.substringAfterLast('/')}'")
                    }

                    withClue("${module.path} imports ${imported.map { it.path }}, declares ${module.requires.map { it.path }}") {
                        module.requires shouldContainExactlyInAnyOrder imported
                    }
                }
            }
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
