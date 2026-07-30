package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.codegen.model.FxGenericMatrix
import io.peekandpoke.ultra.codegen.model.FxHoldsAny
import io.peekandpoke.ultra.codegen.model.FxHoldsInferParam
import io.peekandpoke.ultra.codegen.model.FxHoldsInterface
import io.peekandpoke.ultra.codegen.model.FxHoldsRecordShadow
import io.peekandpoke.ultra.codegen.model.FxHoldsSelfShadow
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxStarList
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.FxTalkId
import io.peekandpoke.ultra.codegen.model.TsProp
import io.peekandpoke.ultra.codegen.model.TsTypeClaim
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TsTypeDecl
import io.peekandpoke.ultra.codegen.model.TsTypeRef
import io.peekandpoke.ultra.codegen.model.TypeId
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.model.TypeWalker
import io.peekandpoke.ultra.codegen.model.other.FxHoldsBothSpeakers
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
        "dangling references" - {

            // Not reachable through the walker by design — it is an internal invariant. Constructing
            // the model by hand is the only way to pin the guard, and the guard is worth having because
            // an unnoticed dangling ref renders as the literal string `unknown` in the output.

            fun modelReferencing(missing: TypeId): TypeModel {
                val owner = TypeId.of(typeOf<FxTalk>())

                return TypeModel(
                    decls = mapOf(
                        owner to TsTypeDecl.Obj(
                            id = owner,
                            name = "FxTalk",
                            props = listOf(TsProp(name = "orphan", type = TsTypeRef.Named(missing))),
                        )
                    ),
                    usedClaims = emptyMap(),
                    unresolved = emptyList(),
                    longValued = emptyList(),
                    undetermined = emptyList(),
                )
            }

            "a reference to something neither declared nor claimed is a problem" {
                val report = TsModelValidator(SlumberConfig.default)
                    .validate(modelReferencing(TypeId.of(typeOf<FxSpeaker>())))

                report.ok shouldBe false

                withClue("it is a generator bug, and the message should say so rather than blame the user") {
                    report.format() shouldContain "walker invariant"
                }
            }

            "the same reference is fine once a claim covers it" {
                val missing = TypeId.of(typeOf<FxSpeaker>())

                val claimed = modelReferencing(missing).copy(
                    usedClaims = mapOf(
                        FxSpeaker::class.qualifiedName!! to TsTypeClaim(
                            qualifiedName = FxSpeaker::class.qualifiedName!!,
                            tsName = "Speaker",
                            importFrom = "./runtime/speaker",
                            schema = "Speaker",
                            opaque = false,
                            reason = "",
                            claimedBy = "acme",
                        )
                    )
                )

                TsModelValidator(SlumberConfig.default).validate(claimed).ok shouldBe true
            }
        }

        "identifiers TypeScript will not accept" - {

            // Escaping (ef5eba72) covers string literals and property keys. Declaration names and type
            // parameters are DIFFERENT positions and were spliced in raw. Generic emission widened the
            // blast radius: an interface body used to be written out only for a recursive declaration,
            // and now every generic declaration has one.

            fun report(type: KType) = TsModelValidator(SlumberConfig.default).validate(walk(type))

            "a class named Record captures the global the emitter uses for every Map" {
                // Emits `entries: Record<string, T>` inside the interface, which binds to the LOCAL
                // Record — tsc reports TS2315, "Type 'Record' is not generic".
                val r = report(typeOf<FxHoldsRecordShadow>())

                r.ok shouldBe false
                r.format() shouldContain "Record"
                r.format() shouldContain "built-in"
            }

            "a type parameter named `infer` is a TypeScript syntax error" {
                // Worse than it looks: a syntax error makes tsc skip semantic checking for the whole
                // file, masking every other problem in it.
                val r = report(typeOf<FxHoldsInferParam>())

                r.ok shouldBe false
                r.format() shouldContain "reserved"
            }

            "a type parameter shadowing its own declaration" {
                report(typeOf<FxHoldsSelfShadow>()).format() shouldContain "shadows the generated type"
            }

            "an ordinary model raises nothing" {
                withClue("this must not fire for well-named code, or it is just noise") {
                    report(typeOf<FxGenericMatrix>()).ok shouldBe true
                }
            }
        }

        "name collisions" - {

            // Both of these checks had ZERO tests before 2026-07-30, so the behaviour being extended
            // here was entirely unpinned.

            "two same-named classes from different packages collide" {
                val report = TsModelValidator(SlumberConfig.default)
                    .validate(walk(typeOf<FxHoldsBothSpeakers>()))

                withClue("TS names are built from simple names, so the packages do not disambiguate") {
                    report.ok shouldBe false
                    report.format() shouldContain "FxSpeaker"
                    report.format() shouldContain "collision"
                }
            }

            "a declaration colliding with a CLAIMED name is caught" {
                // The claim is imported into the same module scope models.ts declares into, so this is
                // TS2440 in the generated file. Nothing reported it before the check knew about claims.
                val claims = TsTypeClaims().apply {
                    scopeFor("acme").map<FxTalkId>(
                        tsName = "FxSpeaker",
                        importFrom = "./runtime/acme",
                        schema = "FxSpeaker",
                    )
                }

                val report = TsModelValidator(SlumberConfig.default)
                    .validate(walk(typeOf<FxTalk>(), claims))

                report.ok shouldBe false

                withClue("the message must name the contributor, or nobody knows who to blame") {
                    report.format() shouldContain "acme"
                }
            }

            "an ordinary model has no collisions" {
                withClue("this must not fire for well-formed input, or it is just noise") {
                    TsModelValidator(SlumberConfig.default).validate(walk(typeOf<FxTalk>())).ok shouldBe true
                }
            }

            "a claim WITHOUT an importFrom brings no name into scope, so it cannot collide" {
                // An inline expression such as `z.unknown()` is not a module-scope binding.
                val claims = TsTypeClaims().apply {
                    scopeFor("acme").map<FxTalkId>(tsName = "FxSpeaker", schema = "z.string()")
                }

                TsModelValidator(SlumberConfig.default)
                    .validate(walk(typeOf<FxTalk>(), claims)).ok shouldBe true
            }
        }

        "an undeterminable position is a BLOCKING problem, not an advisory" - {

            "a star projection fails validation with an actionable message" {
                val report = TsModelValidator(SlumberConfig.default).validate(walk(typeOf<FxStarList>()))

                report.ok shouldBe false

                withClue("`unknown` accepts anything, so the field silently stops being validated") {
                    report.format() shouldContain "not knowable"
                    report.format() shouldContain "claims.opaque"
                }

                withClue("the trail is what makes it actionable") {
                    report.format() shouldContain "root -> rows -> *"
                }
            }

            "claims.opaque remains the deliberate way to ask for unknown" {
                val claims = TsTypeClaims().apply {
                    scopeFor("t").opaque<FxHoldsAny>(reason = "genuinely dynamic")
                }

                val report = TsModelValidator(SlumberConfig.default)
                    .validate(walk(typeOf<FxHoldsAny>(), claims))

                withClue("an escape hatch that cannot be reached is not an escape hatch") {
                    report.ok shouldBe true
                }

                withClue("and it is still reported, unlike the silent degradation it replaces") {
                    report.advisories.map { it.detail }.first() shouldContain "opaque"
                }
            }
        }

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

                val report = TsModelValidator(SlumberConfig.default).validate(model)

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

                val report = TsModelValidator(SlumberConfig.default).validate(model)

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

                val report = TsModelValidator(SlumberConfig.default).validate(model)

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
                    scopeFor("t").map<FxTalkId>(tsName = "X", schema = "x")
                }

                val model = walk(typeOf<FxTalk>(), claims)

                val report = TsModelValidator(SlumberConfig.default).validate(model)

                report.advisories.map { it.detail }.any { it.contains("2^53") } shouldBe true
            }
        }
    }
}
