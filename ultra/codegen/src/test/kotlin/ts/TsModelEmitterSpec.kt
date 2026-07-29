package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.codegen.model.FxCustomCodecType
import io.peekandpoke.ultra.codegen.model.FxEvent
import io.peekandpoke.ultra.codegen.model.FxHoldsClaimed
import io.peekandpoke.ultra.codegen.model.FxMutualA
import io.peekandpoke.ultra.codegen.model.FxNode
import io.peekandpoke.ultra.codegen.model.FxResult
import io.peekandpoke.ultra.codegen.model.FxShape
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxStatus
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.FxTalkId
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TsTypeRef
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.model.TypeWalker
import io.peekandpoke.ultra.codegen.shouldHaveNoDiffs
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class TsModelEmitterSpec : FreeSpec() {

    private fun emit(type: KType, claims: TsTypeClaims = TsTypeClaims()): String {
        val model: TypeModel = TypeWalker(claims).walk(listOf(TypeWalker.Root(type, "root")))

        return TsModelEmitter(model, header = "").emit()
    }

    init {
        "a plain data class emits a schema plus an inferred type" {
            emit(typeOf<FxSpeaker>()) shouldHaveNoDiffs """
                import { z } from 'zod'

                export const FxSpeaker = z.object({
                    name: z.string(),
                    bio: z.string().nullable(),
                })
                export type FxSpeaker = z.infer<typeof FxSpeaker>
            """.trimIndent()
        }

        "an enum emits z.enum over the constant names" {
            emit(typeOf<FxStatus>()) shouldContain "export const FxStatus = z.enum(['ACTIVE', 'ARCHIVED'])"
        }

        "a value class emits an alias to its underlying schema" {
            emit(typeOf<FxTalkId>()) shouldHaveNoDiffs """
                import { z } from 'zod'

                export const FxTalkId = z.string()
                export type FxTalkId = z.infer<typeof FxTalkId>
            """.trimIndent()
        }

        "a sealed hierarchy emits a discriminated union after its variants" {
            val out = emit(typeOf<FxShape>())

            withClue("variants must precede the union — a zod schema is a const, not a hoisted type") {
                out.indexOf("export const FxShapeCircle") shouldBe out.indexOf("export const FxShapeCircle")
                (out.indexOf("export const FxShapeCircle") < out.indexOf("export const FxShape =")) shouldBe true
                (out.indexOf("export const FxShapeSquare") < out.indexOf("export const FxShape =")) shouldBe true
            }

            out shouldContain "z.discriminatedUnion('_type', ["
            out shouldContain "_type: z.literal('"
        }

        "a custom discriminator field is used verbatim" {
            val out = emit(typeOf<FxEvent>())

            out shouldContain "z.discriminatedUnion('kind', ["
            out shouldContain "kind: z.literal('created')"
            out shouldContain "kind: z.literal('deleted')"
        }

        "a plain sealed object variant emits an empty object with only its discriminator" {
            emit(typeOf<FxResult>()) shouldContain "export const FxResultPending = z.object({\n    _type: z.literal("
        }

        "recursion" - {

            "a self-referential type uses z.lazy and an explicit type" {
                val out = emit(typeOf<FxNode>())

                withClue("z.infer cannot see through z.lazy, so the type must be spelled out") {
                    out shouldContain "export type FxNode = {"
                    out shouldContain "export const FxNode: z.ZodType<FxNode> = z.lazy(() => z.object({"
                    out shouldNotContain "export type FxNode = z.infer"
                }

                out shouldContain "children: FxNode[]"
                out shouldContain "parent: FxNode | null"
            }

            "mutually recursive types break the cycle exactly once" {
                val out = emit(typeOf<FxMutualA>())

                withClue("only the one emitted first needs deferring; making both lazy is waste") {
                    val lazies = Regex("z\\.ZodType<(\\w+)> = z\\.lazy\\(")
                        .findAll(out).map { it.groupValues[1] }.toList()

                    lazies.size shouldBe 1
                }
            }

            "no schema references a const that is declared later, unless it is lazy" {
                // The invariant that actually matters: a zod schema is a `const`, so referencing one
                // declared further down the file is a temporal-dead-zone crash at import time.
                listOf(typeOf<FxMutualA>(), typeOf<FxNode>(), typeOf<FxShape>(), typeOf<FxEvent>())
                    .forEach { root ->
                        val out = emit(root)

                        val declared = Regex("export const (\\w+)").findAll(out)
                            .map { it.groupValues[1] }.toList()

                        val positions = declared.withIndex().associate { (i, n) -> n to i }

                        // Split on every `export` and keep only the const chunks. Splitting on
                        // `export const` alone would swallow the NEXT declaration's `export type X = {}`
                        // preamble into this chunk — and type positions are hoisted in TypeScript, so
                        // they are never a temporal-dead-zone risk anyway.
                        val chunks = out.split(Regex("(?=export )"))
                            .filter { it.startsWith("export const ") }

                        chunks.forEach { chunk ->
                            val owner = Regex("export const (\\w+)").find(chunk)!!.groupValues[1]
                            val ownerPos = positions.getValue(owner)
                            val isLazy = chunk.contains("z.lazy(")

                            if (!isLazy) {
                                // Blank out string literals first: a discriminator literal carries the
                                // fully-qualified Kotlin name, so 'pkg.FxShape.Circle' would otherwise
                                // look like a reference to FxShape.
                                val body = chunk.substringAfter("=").replace(Regex("'[^']*'"), "''")

                                declared.filter { positions.getValue(it) > ownerPos }.forEach { later ->
                                    withClue("'$owner' references '$later', which is declared later, without z.lazy") {
                                        Regex("\\b$later\\b").containsMatchIn(body) shouldBe false
                                    }
                                }
                            }
                        }
                    }
            }

            "a non-recursive type is NOT made lazy" {
                emit(typeOf<FxSpeaker>()) shouldNotContain "z.lazy"
            }
        }

        "claims" - {

            "a claimed type is imported, not declared" {
                val claims = TsTypeClaims().apply {
                    scopeFor("t").map<FxCustomCodecType>(
                        tsName = "Stamp",
                        importFrom = "./runtime/stamp",
                        schema = "StampSchema",
                    )
                }

                val out = emit(typeOf<FxHoldsClaimed>(), claims)

                out shouldContain "import { Stamp, StampSchema } from './runtime/stamp'"
                out shouldContain "stamp: StampSchema,"
                out shouldNotContain "export const Stamp ="
            }

            "an opaque claim renders as unknown with no import" {
                val claims = TsTypeClaims().apply {
                    scopeFor("t").opaque<FxCustomCodecType>(reason = "internal")
                }

                val out = emit(typeOf<FxHoldsClaimed>(), claims)

                out shouldContain "stamp: z.unknown(),"
                out shouldNotContain "from './runtime"
            }
        }

        "an optional property is marked optional in both the schema and the type" {
            val out = emit(typeOf<FxTalk>())

            out shouldContain "featured: z.boolean().optional(),"
        }

        "a nullable array element is parenthesised before the array suffix" {
            val model = TypeWalker(TsTypeClaims())
                .walk(listOf(TypeWalker.Root(typeOf<FxNode>(), "root")))

            val renderer = TsRenderer(model)

            withClue("`A | null[]` would parse as A union (null[]) — wrong type entirely") {
                renderer.type(
                    TsTypeRef.ArrayOf(
                        TsTypeRef.Nullable(
                            TsTypeRef.TsString
                        )
                    )
                ) shouldBe "(string | null)[]"
            }
        }
    }
}
