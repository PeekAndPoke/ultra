package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.rest.acl.UserApiAccessMatrix
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import io.peekandpoke.ultra.remote.ApiAccessLevel

/**
 * Guards the hand-written `runtime/acl.ts` against the Kotlin it mirrors.
 *
 * The generated SDK's access lookup is only correct while THREE things agree, and none of them is
 * checked by a compiler: the level union, the key format, and the strings a generated member carries.
 * All three fail silently and in the same direction — `getAccessLevel` returns `Denied` for
 * everything, so every button disappears and nothing reports an error.
 *
 * Lives here rather than in `ultra:codegen` because that module cannot see `ultra:remote`, where
 * [ApiAccessLevel] is declared. The resource is on the classpath either way.
 */
class AclRuntimeParitySpec : FreeSpec() {

    private val aclTs: String by lazy {
        this::class.java.classLoader.getResourceAsStream("ts/runtime/acl.ts")
            ?.bufferedReader()?.readText()
            ?: error("ts/runtime/acl.ts is not on the classpath")
    }

    /** The members of the `ApiAccessLevel` union declared in `acl.ts`. */
    private fun declaredLevels(): List<String> {
        val line = aclTs.lineSequence()
            .firstOrNull { it.startsWith("export type ApiAccessLevel") }
            ?: error("acl.ts no longer declares `export type ApiAccessLevel` on one line")

        return Regex("'([^']+)'").findAll(line).map { it.groupValues[1] }.toList()
    }

    init {
        "the TS level union matches the Kotlin enum, name for name" {
            // A level added on the Kotlin side and missed here does not fail to compile: the server
            // sends a string the union does not list, zod rejects the whole matrix, and the SDK
            // silently denies everything.
            declaredLevels() shouldContainExactlyInAnyOrder ApiAccessLevel.entries.map { it.name }
        }

        "the lookup key format matches ApiAcl's" {
            // `ApiAcl.key` is `"$method|$uri"`. A different separator on either side still produces a
            // Map that works — it just never matches, which reads as "this user may do nothing".
            withClue("acl.ts must build the same `method|uri` key") {
                aclTs shouldContain "\${e.method}|\${e.uri}"
                aclTs shouldContain "\${route.method}|\${route.uri}"
            }
        }

        "a generated member carries exactly the strings the access matrix is keyed by" {
            // THE test that protects the whole feature. `ApiAccessDescriptor` builds its entries from
            // `route.method.value` and `route.pattern.pattern`; the emitter must pass the same two
            // through `route(...)`, or the keys cannot meet.
            val feature = FxDemoApiFeature(listOf(FxTalksApiRoutes(), FxParamApiRoutes()))

            val out = TsSdkBuilder.forTesting(listOf(RestApiTsContributor(lazyOf(listOf(feature)))))
                .build()
                .output.entries()
                .single { it.path.endsWith("fxDemoClient.ts") }
                .content

            val routes = feature.getRouteGroups().flatMap { it.all }

            routes.isEmpty() shouldBe false

            routes.forEach { route ->
                // Exactly the expressions ApiAccessDescriptor uses, side by side with the emitter's.
                val expected = "route('${route.method.value}', '${route.pattern.pattern}',"

                withClue("emitted client must carry `$expected`") {
                    out shouldContain expected
                }
            }
        }

        "the matrix entry the server sends is shaped the way acl.ts reads it" {
            // Cheap, and it pins the two field names the key is built from. Renaming either on the
            // Kotlin side compiles fine and breaks every lookup.
            val entry = UserApiAccessMatrix.Entry(
                method = "GET",
                uri = "/x/{id}",
                level = ApiAccessLevel.Granted,
            )

            entry.method shouldBe "GET"
            entry.uri shouldBe "/x/{id}"
            entry.level shouldBe ApiAccessLevel.Granted
        }
    }
}
