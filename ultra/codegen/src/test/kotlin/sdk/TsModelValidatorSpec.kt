package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.codegen.model.FxHoldsInterface
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.model.TypeWalker
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.slumber.SlumberConfig
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** A type reaching a Slumber-custom-coded value — the case the whole validation phase exists for. */
data class FxHoldsInstant(val at: MpInstant)

class TsModelValidatorSpec : FreeSpec() {

    private fun walk(type: KType, claims: TsTypeClaims = TsTypeClaims()): TypeModel =
        TypeWalker(claims).walk(listOf(TypeWalker.Root(type, "root")))

    init {
        "the custom-codec check — the reason this phase exists" - {

            "an UNCLAIMED type with a custom Slumber codec fails validation" {
                val model = walk(typeOf<FxHoldsInstant>())

                val report = TsModelValidator(SlumberConfig.default).validate(model)

                withClue("MpInstant slumbers to {ts, timezone, human} — nothing in its KType says so") {
                    report.ok shouldBe false
                }

                val message = report.format()

                message shouldContain MpInstant::class.qualifiedName!!
                message shouldContain "custom Slumber codec"
                message shouldContain "MpInstantSlumberer"
                message shouldContain "claims.map"
            }

            "the same model passes once the type is claimed" {
                val claims = TsTypeClaims().apply {
                    scopeFor("ultra:datetime").map<MpInstant>(
                        tsName = "MpInstant",
                        importFrom = "./runtime/datetime",
                        schema = "MpInstantSchema",
                    )
                }

                val model = walk(typeOf<FxHoldsInstant>(), claims)

                TsModelValidator(SlumberConfig.default).validate(model).ok shouldBe true
            }

            "a plain structural model passes" {
                val model = walk(typeOf<FxSpeaker>())

                val report = TsModelValidator(SlumberConfig.default).validate(model)

                withClue(report.format()) { report.ok shouldBe true }
            }
        }

        "unresolved types" - {

            "are reported with the path that reached them" {
                val model = walk(typeOf<FxHoldsInterface>())

                val report = TsModelValidator().validate(model)

                report.ok shouldBe false
                report.format() shouldContain "reached via: root -> thing"
            }
        }

        "claims" - {

            "a non-opaque claim with no zod schema is a problem" {
                val claims = TsTypeClaims().apply {
                    scopeFor("test").map<MpInstant>(tsName = "MpInstant", importFrom = "./rt")
                }

                val model = walk(typeOf<FxHoldsInstant>(), claims)

                val report = TsModelValidator().validate(model)

                withClue("z.infer needs a schema to infer from") {
                    report.ok shouldBe false
                    report.format() shouldContain "no zod schema"
                }
            }

            "an opaque claim needs no schema and is reported as an advisory" {
                val claims = TsTypeClaims().apply {
                    scopeFor("test").opaque<MpInstant>(reason = "not needed by the frontend")
                }

                val model = walk(typeOf<FxHoldsInstant>(), claims)

                val report = TsModelValidator().validate(model)

                report.ok shouldBe true
                report.advisories.map { it.subject } shouldBe listOf(MpInstant::class.qualifiedName)
            }

            "claiming twice is a hard error naming both contributors" {
                val claims = TsTypeClaims()
                claims.scopeFor("contributor-a").map<MpInstant>(tsName = "A")

                val thrown = runCatching {
                    claims.scopeFor("contributor-b").map<MpInstant>(tsName = "B")
                }.exceptionOrNull()

                thrown!!.message!! shouldContain "contributor-a"
                thrown.message!! shouldContain "contributor-b"
            }
        }

        "advisories" - {

            "a reachable Long is reported but does not fail the build" {
                val claims = TsTypeClaims().apply {
                    scopeFor("t").map<io.peekandpoke.ultra.codegen.model.FxTalkId>(tsName = "X", schema = "x")
                }

                val model = walk(typeOf<FxTalk>(), claims)

                val report = TsModelValidator().validate(model)

                report.advisories.map { it.detail }.any { it.contains("2^53") } shouldBe true
            }
        }
    }
}
