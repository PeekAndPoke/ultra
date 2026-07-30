package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.codegen.model.FxAlphaLeaf
import io.peekandpoke.ultra.codegen.model.FxCustomCodecType
import io.peekandpoke.ultra.codegen.model.FxEvent
import io.peekandpoke.ultra.codegen.model.FxHoldsClaimed
import io.peekandpoke.ultra.codegen.model.FxIdHolder
import io.peekandpoke.ultra.codegen.model.FxMutualA
import io.peekandpoke.ultra.codegen.model.FxNode
import io.peekandpoke.ultra.codegen.model.FxPartlyClaimed
import io.peekandpoke.ultra.codegen.model.FxQuoted
import io.peekandpoke.ultra.codegen.model.FxResult
import io.peekandpoke.ultra.codegen.model.FxShape
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxStatus
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.FxTalkId
import io.peekandpoke.ultra.codegen.model.FxZeta
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

        "wire strings that are not identifier-shaped are escaped and quoted" {
            val out = emit(typeOf<FxQuoted>())

            withClue("the child identifier carries a quote and a backslash") {
                out shouldContain """z.literal('O\'Brien\\Co')"""
            }

            withClue("a discriminator field is a string constant, so it needs quoting like any prop") {
                out shouldContain """'@type': z.literal("""
                out shouldContain """z.discriminatedUnion('@type', ["""
            }

            withClue("a property name needing both quoting and escaping gets both") {
                out shouldContain """'it\'s': z.string()"""
            }

            withClue("a property name needing only quoting is not escaped") {
                out shouldContain """'dashed-name': z.string()"""
            }

            withClue("nothing raw survives — every apostrophe in the output is escaped or a delimiter") {
                // A raw `'` inside a literal is what breaks the file; the delimiters are the only
                // apostrophes allowed to stand alone.
                out shouldNotContain """literal('O'Brien"""
            }
        }

        "a claimed polymorphic child is imported and referenced by its claimed name" {
            val claims = TsTypeClaims().apply {
                scopeFor("test").map<FxPartlyClaimed.Custom>(
                    tsName = "CustomVariant",
                    importFrom = "./runtime/custom",
                    schema = "CustomVariant",
                )
            }

            val out = emit(typeOf<FxPartlyClaimed>(), claims)

            withClue("union variants are enqueued directly, so the claim is recorded while DECLARING") {
                out shouldContain "import { CustomVariant } from './runtime/custom'"
            }

            withClue("an unrecorded claim renders as the literal string 'unknown'") {
                // TsRenderer.nameOf falls through decls, then usedClaims, then returns "unknown" —
                // so a missing usedClaims entry produces a union over a type that does not exist.
                out shouldNotContain "unknown"
                out shouldContain "CustomVariant"
            }

            withClue("the unclaimed sibling is still declared normally") {
                out shouldContain "export const FxPartlyClaimedPlain = z.object({"
            }
        }

        "a claimed union variant uses its SCHEMA expression in schema position" {
            val claims = TsTypeClaims().apply {
                scopeFor("test").map<FxPartlyClaimed.Custom>(
                    tsName = "CustomType",
                    importFrom = "./runtime/custom",
                    schema = "customSchema",
                )
            }

            val out = emit(typeOf<FxPartlyClaimed>(), claims)

            // Asserting only `out shouldContain "customSchema"` passes for the WRONG reason: the
            // import line lists both the tsName and the schema. The union line is the one under test.
            withClue("z.discriminatedUnion takes SCHEMAS; a tsName is a type and is not a value") {
                out shouldContain "z.discriminatedUnion('_type', [customSchema, FxPartlyClaimedPlain])"
                out shouldNotContain "[CustomType,"
            }

            withClue("the type name is still what belongs in the import and in type position") {
                out shouldContain "import { CustomType, customSchema } from './runtime/custom'"
            }
        }

        "a recursive union uses type names in type position and schemas in schema position" {
            // The z.lazy branch is the ONLY one that writes `export type X = A | B`, so it is the only
            // place a variant's TYPE name is used. A claim is what makes the two differ at all.
            val claims = TsTypeClaims().apply {
                scopeFor("test").map<FxAlphaLeaf>(
                    tsName = "LeafType",
                    importFrom = "./runtime/leaf",
                    schema = "leafSchema",
                )
            }

            val out = emit(typeOf<FxZeta>(), claims)

            withClue("type position takes the tsName — a schema expression is not a type") {
                out shouldContain "export type FxZeta = FxAlphaBranch | LeafType"
            }

            withClue("schema position takes the schema — a tsName is not a value") {
                out shouldContain "z.union([FxAlphaBranch, leafSchema])"
            }
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

            "a value class on a cycle is deferred like any other recursive declaration" {
                val out = emit(typeOf<FxIdHolder>())

                withClue("FxIdHolder sorts before FxIds, so the ALIAS is emitted first and must defer") {
                    out shouldContain "export const FxIds: z.ZodType<FxIds> = z.lazy(() =>"
                    out shouldContain "export type FxIds ="
                }

                withClue("z.infer cannot see through z.lazy, so the alias cannot infer its own type") {
                    out shouldNotContain "export type FxIds = z.infer"
                }
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
                listOf(
                    typeOf<FxMutualA>(), typeOf<FxNode>(), typeOf<FxShape>(), typeOf<FxEvent>(),
                    // An ALIAS on a cycle — a shape none of the others cover.
                    typeOf<FxIdHolder>(),
                )
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
