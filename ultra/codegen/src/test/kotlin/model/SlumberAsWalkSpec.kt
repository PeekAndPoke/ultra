package io.peekandpoke.ultra.codegen.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.reflect.typeOf

/**
 * The walker reads `@Slumber.As` when a custom-coded type declares its own wire shape.
 *
 * Purely additive: a CLAIM still wins, so nothing a contributor controls changes. What this removes
 * is the requirement to hand-write a claim for a type that already says what it looks like.
 */
class SlumberAsWalkSpec : FreeSpec() {

    private fun walk(claims: TsTypeClaims = TsTypeClaims(), type: kotlin.reflect.KType) =
        TypeWalker(claims).walk(listOf(TypeWalker.Root(type, "root")))

    init {
        "a declared wire shape is walked INSTEAD of the declaring type" {
            val model = walk(type = typeOf<FxHoldsStamp>())

            withClue("the declared shape is declared; the declaring type is not") {
                model.decls.values.map { it.name } shouldContainExactlyInAnyOrder
                        listOf("FxHoldsStamp", "FxStampRaw")
            }

            val prop = (model.declFor(FxHoldsStamp::class) as TsTypeDecl.Obj).props.single()

            (prop.type as TsTypeRef.Named).id.cls shouldBe FxStampRaw::class
        }

        "a declared SCALAR shape resolves to that scalar" {
            // The MpLocalTime (`As(Long::class)`) and MpTimezone (`As(String::class)`) case.
            val model = walk(type = typeOf<FxHoldsScalarShapes>())

            val props = (model.declFor(FxHoldsScalarShapes::class) as TsTypeDecl.Obj)
                .props.associate { it.name to it.type }

            props["ticks"] shouldBe TsTypeRef.TsNumber
            props["zone"] shouldBe TsTypeRef.TsString

            withClue("neither declaring type is emitted — they ARE their declared shapes") {
                model.decls.values.map { it.name } shouldContainExactlyInAnyOrder listOf("FxHoldsScalarShapes")
            }
        }

        "a CLAIM still wins over the annotation" {
            // A claim is a downstream override for a type we may not own. Silently preferring the
            // annotation would change output that contributors control.
            val claims = TsTypeClaims()

            claims.scopeFor("test").map(
                cls = FxStamp::class,
                tsName = "ClaimedStamp",
                importFrom = "./custom.ts",
                schema = "ClaimedStamp",
            )

            val model = walk(claims = claims, type = typeOf<FxHoldsStamp>())

            withClue("the claim is used, so the declared shape is never walked") {
                model.decls.values.map { it.name } shouldContainExactlyInAnyOrder listOf("FxHoldsStamp")
                model.usedClaims.keys shouldContainExactlyInAnyOrder listOf(FxStamp::class.qualifiedName)
            }
        }

        "a type declaring ITSELF is refused rather than followed" {
            val thrown = runCatching { walk(type = typeOf<FxHoldsSelfDeclared>()) }.exceptionOrNull()

            withClue("following it would never terminate") {
                thrown!!.message!! shouldContain "itself"
            }
        }
    }
}
