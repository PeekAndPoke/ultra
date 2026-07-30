@file:OptIn(ExperimentalCompilerApi::class)

package io.peekandpoke.funktor.rest.auth

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.ByteArrayOutputStream

/**
 * Locks the compile-time guarantees of the auth-rule DSL: the shared @DslMarker on the builder
 * family makes wrong-level and cross-family implicit access a COMPILE ERROR, not a silent append
 * at the wrong nesting level. See `.claude/tasks/20260722-authorize-rule-builder.md`, inventory #3.
 */
class AuthRuleDslCompileSpec : StringSpec({

    fun compile(source: String): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("DslCase.kt", source))
            inheritClassPath = true
            messageOutputStream = ByteArrayOutputStream()
        }.compile()
    }

    "positive control: valid nested DSL usage compiles" {
        val result = compile(
            """
            package dslcase

            import io.peekandpoke.funktor.rest.auth.RootAuthRuleBuilder

            fun case(builder: RootAuthRuleBuilder<Unit, Unit>) {
                builder.apply {
                    forAny {
                        isSuperUser()
                        forAll {
                            forUserType("B2bUser")
                            forRole("org-admin")
                        }
                    }
                }
            }
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    "root-only public() inside forAny {} does NOT compile (no silent outer-receiver append)" {
        val result = compile(
            """
            package dslcase

            import io.peekandpoke.funktor.rest.auth.RootAuthRuleBuilder

            fun case(builder: RootAuthRuleBuilder<Unit, Unit>) {
                builder.apply {
                    forAny {
                        public()
                    }
                }
            }
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        // Lock the REASON: it must be the DslMarker receiver restriction, not an unresolved
        // reference (which would keep this test green if public() were simply removed).
        result.messages shouldContain "implicit receiver"
    }

    "cross-family implicit access — docs {} inside authorize {} — does NOT compile" {
        // The ApiRoute must be an OUTER RECEIVER (as it is inside real `mount {}` blocks) — that
        // is the hazard shape: without the shared marker, `docs {}` would resolve against the
        // outer route receiver and silently produce a discarded copy.
        val result = compile(
            """
            package dslcase

            import io.peekandpoke.funktor.rest.ApiRoute
            import io.peekandpoke.funktor.rest.docs.docs

            fun case(route: ApiRoute.Plain<Unit>) {
                route.apply {
                    authorize {
                        isSuperUser()
                        docs {
                            name = "smuggled"
                        }
                    }
                }
            }
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "implicit receiver"
    }

    "control for the cross-family case: docs {} chained OUTSIDE authorize {} compiles" {
        val result = compile(
            """
            package dslcase

            import io.peekandpoke.funktor.rest.ApiRoute
            import io.peekandpoke.funktor.rest.docs.docs

            fun case(route: ApiRoute.Plain<Unit>) {
                route.apply {
                    docs {
                        name = "legit"
                    }.authorize {
                        isSuperUser()
                    }
                }
            }
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }
})
