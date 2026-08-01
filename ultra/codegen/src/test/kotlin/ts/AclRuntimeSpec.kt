package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.codegen.model.TsTypeRef
import io.peekandpoke.ultra.remote.ApiAccessLevel

/**
 * Guards the hand-written `runtime/route.ts` and `runtime/acl.ts` against the Kotlin they mirror.
 *
 * Both files declare a CLOSED union that a Kotlin declaration is the source of truth for, and neither
 * pairing is checked by a compiler. Lives in `ultra:codegen` because that is where the resources
 * live — `ultra/codegen/build.gradle.kts:44` puts `ultra:remote` on the test classpath for exactly
 * this. The route-identity half, which needs a real `ApiFeature`, stays in `funktor:codegen`.
 */
class AclRuntimeSpec : FreeSpec() {

    private fun resource(path: String): String =
        this::class.java.classLoader.getResourceAsStream(path)
            ?.bufferedReader()?.readText()
            ?: error("$path is not on the classpath")

    private val aclTs: String by lazy { resource("ts/runtime/acl.ts") }
    private val routeTs: String by lazy { resource("ts/runtime/route.ts") }

    /** The members of a single-line `export type <name> = 'a' | 'b'` union. */
    private fun unionOf(source: String, name: String): List<String> {
        val line = source.lineSequence()
            .firstOrNull { it.startsWith("export type $name") }
            ?: error("no single-line `export type $name` declaration found")

        return Regex("'([^']+)'").findAll(line).map { it.groupValues[1] }.toList()
    }

    init {
        "acl.ts" - {

            "its level union matches the Kotlin enum, name for name" {
                // A level added on the Kotlin side and missed here does not fail to compile: the
                // server sends a string the union does not list, zod rejects the whole matrix, and
                // the SDK denies everything — silently, because denial is the safe default.
                unionOf(aclTs, "AccessLevel") shouldContainExactlyInAnyOrder
                        ApiAccessLevel.entries.map { it.name }
            }

            "it must NOT export the generated model's name" {
                // THE regression test for the barrel break. An SDK reaching the auth feature emits
                // `ApiAccessLevel` into models.ts as BOTH a const and a type, and the barrel
                // `export *`s every module — so exporting that name here is TS2308 in exactly the
                // SDKs this file exists to serve. Found in review, 2026-08-01.
                withClue("acl.ts must not re-use a name models.ts generates") {
                    aclTs shouldNotContain "export type ApiAccessLevel"
                    aclTs shouldNotContain "export const ApiAccessLevel"
                }
            }

            "its lookup key is built the same way on both sides" {
                // Cross-language parity of the separator is NOT the point and is unobservable — each
                // implementation builds a private Map and never compares a key with the other's. What
                // matters is that the constructor and the lookup inside THIS file agree; a one-sided
                // edit yields a Map that can never match, i.e. "this user may do nothing".
                aclTs shouldContain "\${e.method}|\${e.uri}"
                aclTs shouldContain "\${route.method}|\${route.uri}"
            }
        }

        "route.ts" - {

            "its HttpMethod union matches the emitter's guard, name for name" {
                // The second closed union this feature introduced. `KNOWN_HTTP_METHODS` exists to
                // refuse an unlisted method at GENERATION time; if the two drift, adding a method to
                // the Kotlin side alone emits `route('TRACE', ...)` and the consuming app fails with
                // TS2345 inside a file it cannot edit — the exact outcome the guard prevents.
                unionOf(routeTs, "HttpMethod") shouldContainExactlyInAnyOrder
                        TsClientSpec.Endpoint.KNOWN_HTTP_METHODS.toList()
            }
        }

        "the emitter refuses a method HttpMethod does not list" {
            // The reject path itself, which nothing exercised.
            val thrown = shouldThrow<IllegalArgumentException> {
                TsClientSpec.Endpoint(
                    member = "trace",
                    httpMethod = "TRACE",
                    pattern = "/x",
                    responseRef = TsTypeRef.TsString,
                    doc = null,
                )
            }

            thrown.message!! shouldContain "TRACE"

            withClue("the message must name the member and point at the union to widen") {
                thrown.message!! shouldContain "trace"
                thrown.message!! shouldContain "runtime/route.ts"
            }
        }

        "a lowercase method is refused too" {
            // `route.method.value` is uppercase for every ktor HttpMethod constant, but the guard
            // must not silently accept a hand-built spec that differs only in case — the matrix key
            // is compared verbatim.
            shouldThrow<IllegalArgumentException> {
                TsClientSpec.Endpoint(
                    member = "getIt",
                    httpMethod = "get",
                    pattern = "/x",
                    responseRef = TsTypeRef.TsString,
                    doc = null,
                )
            }
        }

        "a listed method is accepted" {
            TsClientSpec.Endpoint.KNOWN_HTTP_METHODS.forEach { method ->
                TsClientSpec.Endpoint(
                    member = "m",
                    httpMethod = method,
                    pattern = "/x",
                    responseRef = TsTypeRef.TsString,
                    doc = null,
                ).httpMethod shouldBe method
            }
        }
    }
}
