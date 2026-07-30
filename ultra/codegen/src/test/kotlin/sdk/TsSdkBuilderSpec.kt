package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import io.peekandpoke.ultra.codegen.contributors.MpDateTimeTsContributor
import io.peekandpoke.ultra.codegen.model.FxNode
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TsTypeRef
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.slumber.SlumberConfig
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** A type reaching a custom-coded value, so the datetime contributor is actually needed. */
private data class HoldsInstant(val at: MpInstant)

/** Contributes a root and nothing else. */
private class RootContributor(
    override val name: String,
    private val type: KType,
) : TsSdkContributor {
    // Qualified by the contributor's name because root labels must be unique across the whole run —
    // they are how a contributor asks for its root's resolved reference at emit time.
    override fun contribute(roots: TsSdkRoots) = roots.root(type, "$name:root")
}

/** Roots one type under a caller-chosen label, so a collision can be provoked deliberately. */
private class FixedLabelContributor(
    override val name: String,
    private val type: KType,
    private val label: String,
) : TsSdkContributor {
    override fun contribute(roots: TsSdkRoots) = roots.root(type, label)
}

/** Writes one file during emit. */
private class FileContributor(
    override val name: String,
    private val path: String,
) : TsSdkContributor {
    override fun contribute(roots: TsSdkRoots) = roots.root(typeOf<FxSpeaker>(), "$name:root")
    override fun emit(context: TsSdkEmitContext) = context.out.file(path, "// $name")
}

class TsSdkBuilderSpec : FreeSpec() {

    init {
        "phases" - {

            "claims are collected before roots are walked, whatever the contributor order" {
                // The claiming contributor is registered LAST here and FIRST in the next case. If
                // phases were not global, one of the two orders would fail to resolve MpInstant.
                val a = TsSdkBuilder(
                    contributors = listOf(
                        RootContributor("roots", typeOf<HoldsInstant>()),
                        MpDateTimeTsContributor(),
                    ),
                    slumberConfig = SlumberConfig.default,
                ).build()

                val b = TsSdkBuilder(
                    contributors = listOf(
                        MpDateTimeTsContributor(),
                        RootContributor("roots", typeOf<HoldsInstant>()),
                    ),
                    slumberConfig = SlumberConfig.default,
                ).build()

                withClue("contributor order must not change the generated output at all") {
                    a.output.entries().map { it.path to it.content } shouldBe
                            b.output.entries().map { it.path to it.content }
                }
            }

            "two contributors BOTH supplying roots produce byte-identical output in either order" {
                // The claim test above only had one contributor supplying roots, so it could not
                // catch this: the SET of declarations does not depend on contributor order, but the
                // discovery ORDER does, and emitting in discovery order made the file differ between
                // runs. Invisible to a compiler, fatal to `--check`.
                fun run(vararg contributors: TsSdkContributor): String =
                    TsSdkBuilder.forTesting(contributors.toList())
                        .build().output.entries().first { it.path == "models.ts" }.content

                val alpha = RootContributor("alpha", typeOf<FxSpeaker>())
                val beta = RootContributor("beta", typeOf<FxNode>())

                withClue("emission order must be stable, or --check reports drift that is not real") {
                    run(alpha, beta) shouldBe run(beta, alpha)
                }
            }

            "an unclaimed custom-coded type fails before anything is emitted" {
                val thrown = runCatching {
                    TsSdkBuilder(
                        contributors = listOf(RootContributor("roots", typeOf<HoldsInstant>())),
                        slumberConfig = SlumberConfig.default,
                    ).build()
                }.exceptionOrNull()

                withClue("validation must run before emit, so a failure leaves no partial output") {
                    thrown!!.message!! shouldContain "MpInstantSlumberer"
                }
            }

            "models.ts is always emitted" {
                val result = TsSdkBuilder
                    .forTesting(listOf(RootContributor("roots", typeOf<FxSpeaker>())))
                    .build()

                result.output.entries().map { it.path } shouldContainExactly listOf("models.ts")
            }
        }

        "runtime resources" - {

            "the datetime runtime ships only when a datetime type is actually reachable" {
                val withDates = TsSdkBuilder(
                    contributors = listOf(
                        RootContributor("roots", typeOf<HoldsInstant>()),
                        MpDateTimeTsContributor(),
                    ),
                    slumberConfig = SlumberConfig.default,
                ).build()

                withDates.output.entries().map { it.path } shouldContainExactly
                        listOf("models.ts", "runtime/datetime.ts")

                val withoutDates = TsSdkBuilder(
                    contributors = listOf(
                        RootContributor("roots", typeOf<FxSpeaker>()),
                        MpDateTimeTsContributor(),
                    ),
                    slumberConfig = SlumberConfig.default,
                ).build()

                withClue("an unreachable claim must not drag dead runtime code into the SDK") {
                    withoutDates.output.entries().map { it.path } shouldContainExactly listOf("models.ts")
                }
            }

            "the emitted runtime is the checked-in resource, verbatim" {
                val result = TsSdkBuilder(
                    contributors = listOf(
                        RootContributor("roots", typeOf<HoldsInstant>()),
                        MpDateTimeTsContributor(),
                    ),
                    slumberConfig = SlumberConfig.default,
                ).build()

                val emitted = result.output.entries().first { it.path == "runtime/datetime.ts" }.content

                emitted shouldContain "export const MpLocalTime = z.number()"
                emitted shouldContain "HAND-WRITTEN AND CHECKED IN"
            }
        }

        "conflicts" - {

            "two contributors writing the same path fail, naming both" {
                val thrown = runCatching {
                    TsSdkBuilder.forTesting(
                        listOf(
                            FileContributor("alpha", "clients/Same.ts"),
                            FileContributor("beta", "clients/Same.ts"),
                        ),
                    ).build()
                }.exceptionOrNull()

                thrown!!.message!! shouldContain "alpha"
                thrown.message!! shouldContain "beta"
            }

            "two contributors claiming the same type fail, naming both" {
                val claims = TsTypeClaims()
                claims.scopeFor("first").map<MpInstant>(tsName = "A", schema = "A")

                val thrown = runCatching {
                    claims.scopeFor("second").map<MpInstant>(tsName = "B", schema = "B")
                }.exceptionOrNull()

                thrown!!.message!! shouldContain "first"
                thrown.message!! shouldContain "second"
            }

            "duplicate contributor names are rejected" {
                val thrown = runCatching {
                    TsSdkBuilder.forTesting(
                        listOf(
                            RootContributor("same", typeOf<FxSpeaker>()),
                            RootContributor("same", typeOf<FxSpeaker>()),
                        ),
                    ).build()
                }.exceptionOrNull()

                thrown!!.message!! shouldContain "unique"
            }

            "two contributors using the same root label fail, naming both" {
                // A label is how a contributor asks for its root's resolved reference at emit time,
                // so a duplicate silently hands one contributor the other's type. Found for real: the
                // helpers in this spec both labelled their root "root" until this check was added.
                val thrown = runCatching {
                    TsSdkBuilder.forTesting(
                        listOf(
                            FixedLabelContributor("alpha", typeOf<FxSpeaker>(), "shared"),
                            FixedLabelContributor("beta", typeOf<FxNode>(), "shared"),
                        ),
                    ).build()
                }.exceptionOrNull()

                thrown!!.message!! shouldContain "'shared'"
                thrown.message!! shouldContain "alpha"
                thrown.message!! shouldContain "beta"
            }
        }

        "root references" - {

            "a root's resolved reference is reachable by its label" {
                // A root is a POSITION, not a declaration — `List<FxSpeaker>` declares nothing of its
                // own — so an endpoint returning one can only be rendered from this reference.
                val model = TsSdkBuilder
                    .forTesting(listOf(RootContributor("listy", typeOf<List<FxSpeaker>>())))
                    .build().model

                val ref = model.refForRoot("listy:root")

                withClue("the root is a list, so the ref must be an array, not the element itself") {
                    ref should beInstanceOf<TsTypeRef.ArrayOf>()
                }

                val item = (ref as TsTypeRef.ArrayOf).item

                (item as TsTypeRef.Named).id.cls shouldBe FxSpeaker::class
            }

            "a nullable root keeps its nullability on the reference" {
                val model = TsSdkBuilder
                    .forTesting(listOf(RootContributor("maybe", typeOf<FxSpeaker?>())))
                    .build().model

                model.refForRoot("maybe:root") should beInstanceOf<TsTypeRef.Nullable>()
            }

            "asking for a label that was never added is an error naming the known labels" {
                val model = TsSdkBuilder
                    .forTesting(listOf(RootContributor("only", typeOf<FxSpeaker>())))
                    .build().model

                val thrown = runCatching { model.refForRoot("nope") }.exceptionOrNull()

                thrown!!.message!! shouldContain "only:root"
            }
        }

        "guards" - {

            "no contributors at all is an error, not an empty SDK" {
                runCatching { TsSdkBuilder.forTesting(emptyList()).build() }
                    .exceptionOrNull()!!.message!! shouldContain "No TsSdkContributor"
            }

            "contributors that supply no roots is an error, not an empty SDK" {
                val silent = object : TsSdkContributor {
                    override val name = "silent"
                }

                runCatching { TsSdkBuilder.forTesting(listOf(silent)).build() }
                    .exceptionOrNull()!!.message!! shouldContain "supplied any root type"
            }

            "the codec-parity check runs on the path that passes no config explicitly" {
                // MpInstant goes through a custom codec, so an unclaimed one must fail. The point is
                // WHICH entry point is used: `slumberConfig` used to default to null, so the module's
                // headline check — the only thing standing between a custom codec and a schema that
                // does not describe the wire — was off unless a caller opted in.
                // Assert the CODEC name, not the type name. "MpInstant" alone also appears in an
                // unresolved-type message, so it passes whether or not the parity check ran — verified
                // by mutation, the looser assertion survived. Only this phase names the slumberer.
                runCatching {
                    TsSdkBuilder.forTesting(listOf(RootContributor("roots", typeOf<HoldsInstant>())))
                        .build()
                }.exceptionOrNull()!!.message!! shouldContain "MpInstantSlumberer"
            }
        }

        "advisories are returned rather than thrown" {
            val result = TsSdkBuilder(
                contributors = listOf(RootContributor("roots", typeOf<FxTalk>())),
                slumberConfig = SlumberConfig.default,
            ).build()

            withClue("a reachable Long is worth reporting but must not fail the build") {
                result.advisories.any { it.detail.contains("2^53") } shouldBe true
            }
        }
    }
}
