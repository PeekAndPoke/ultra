package io.peekandpoke.ultra.codegen.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.codegen.model.FxHoldsSame
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Generic types are emitted GENERICALLY: one declaration per class, with type arguments carried on the
 * reference rather than baked into a monomorphized name.
 *
 * This replaces monomorphization (`PageOf<Talk>` → `PageOfTalk`), whose justification — that `z.infer`
 * cannot see through a function-valued schema — was measured and found false. See the generics spike in
 * `20260729-ts-sdk-codegen.md`.
 */
class GenericsSpec : FreeSpec() {

    private fun walk(vararg types: KType): TypeModel =
        TypeWalker(TsTypeClaims()).walk(types.map { TypeWalker.Root(it, "root") })

    private fun TypeModel.names(): List<String> = decls.values.map { it.name }

    private fun TypeModel.declOf(cls: KClass<*>): TsTypeDecl = declFor(cls)!!

    private fun TypeModel.objOf(cls: KClass<*>): TsTypeDecl.Obj =
        declOf(cls).shouldBeInstanceOf<TsTypeDecl.Obj>()

    private fun TypeModel.propOf(cls: KClass<*>, prop: String): TsTypeRef =
        objOf(cls).props.first { it.name == prop }.type

    init {
        "a generic used as a ROOT resolves to its declaration" - {

            // Phase 2's shape: a paged endpoint roots `PageOf<Talk>` directly. Declarations are keyed
            // by CLASS, so an id built from an instantiation never matches one — `TsFixtureGenerator`
            // already made this lookup and survived only because no fixture used a generic root.

            "declFor finds it from an instantiated KType" {
                val model = walk(typeOf<FxPageOf<FxTalk>>())

                model.declFor(typeOf<FxPageOf<FxTalk>>())
                    .shouldBeInstanceOf<TsTypeDecl.Obj>()
                    .typeParams shouldContainExactly listOf("T")
            }

            "the raw id lookup does NOT match, which is why declFor exists" {
                val model = walk(typeOf<FxPageOf<FxTalk>>())

                withClue("TypeId.of carries the arguments; a declaration id carries only the class") {
                    model.decls[TypeId.of(typeOf<FxPageOf<FxTalk>>())] shouldBe null
                }
            }

            "two different instantiations both resolve to the one declaration" {
                val model = walk(typeOf<FxPageOf<FxTalk>>(), typeOf<FxPageOf<FxSpeaker>>())

                model.declFor(typeOf<FxPageOf<FxTalk>>()) shouldBe
                        model.declFor(typeOf<FxPageOf<FxSpeaker>>())

                withClue("and both payloads must still be reachable") {
                    model.declFor(FxTalk::class).shouldNotBeNull()
                    model.declFor(FxSpeaker::class).shouldNotBeNull()
                }
            }

            "an unreached class resolves to null rather than throwing" {
                walk(typeOf<FxSpeaker>()).declFor(FxTalk::class) shouldBe null
            }
        }

        "the whole shape matrix resolves with nothing left over" {
            val model = walk(typeOf<FxGenericMatrix>())

            withClue("an unresolved or undetermined position would mean `unknown` in the output") {
                model.unresolved shouldContainExactly emptyList()
                model.undetermined shouldContainExactly emptyList()
            }
        }

        "one declaration per generic class" - {

            "the same generic at many instantiations is declared ONCE" {
                val model = walk(typeOf<FxGenericMatrix>())

                withClue("FxBox is instantiated 9 times in the matrix") {
                    model.decls.keys.count { it.key == FxBox::class.qualifiedName } shouldBe 1
                }
            }

            "the declaration keeps its type parameters, in order" {
                walk(typeOf<FxGenericMatrix>()).objOf(FxTriple::class)
                    .typeParams shouldContainExactly listOf("A", "B", "C")
            }

            "a non-generic type reports no parameters" {
                walk(typeOf<FxSpeaker>()).objOf(FxSpeaker::class).typeParams shouldContainExactly emptyList()
            }

            "the name carries no arguments — those live on the reference" {
                withClue("monomorphization would have produced FxBoxFxSpeaker here") {
                    walk(typeOf<FxGenericMatrix>()).objOf(FxBox::class).name shouldBe "FxBox"
                }
            }

            "two instantiations produce ONE declaration, not two" {
                val model = walk(typeOf<FxTwoInstantiations>())

                model.names() shouldContainExactlyInAnyOrder listOf(
                    "FxSpeaker", "FxBox", "FxStatus", "FxTwoInstantiations",
                )
            }
        }

        "a generic body refers to its own parameters" - {

            "a bare parameter" {
                walk(typeOf<FxGenericMatrix>()).propOf(FxBox::class, "item") shouldBe
                        TsTypeRef.TypeParam("T")
            }

            "a parameter inside a container" {
                walk(typeOf<FxGenericMatrix>()).propOf(FxPageOf::class, "items") shouldBe
                        TsTypeRef.ArrayOf(TsTypeRef.TypeParam("T"))
            }

            "a NULLABLE parameter position" {
                withClue("`val value: T?` is nullable in the declaration, independent of the argument") {
                    walk(typeOf<FxGenericMatrix>()).propOf(FxMaybe::class, "value") shouldBe
                            TsTypeRef.Nullable(TsTypeRef.TypeParam("T"))
                }
            }

            "each parameter of a multi-parameter type maps to its own" {
                val decl = walk(typeOf<FxGenericMatrix>()).objOf(FxTriple::class)

                decl.props.map { it.type } shouldContainExactly listOf(
                    TsTypeRef.TypeParam("A"), TsTypeRef.TypeParam("B"), TsTypeRef.TypeParam("C"),
                )
            }

            "a generic VALUE class aliases to its parameter" {
                walk(typeOf<FxGenericMatrix>()).declOf(FxWrapped::class)
                    .shouldBeInstanceOf<TsTypeDecl.Alias>()
                    .target shouldBe TsTypeRef.TypeParam("T")
            }
        }

        "arguments ride on the reference" - {

            fun refIn(prop: String): TsTypeRef =
                walk(typeOf<FxGenericMatrix>()).propOf(FxGenericMatrix::class, prop)

            fun named(cls: KClass<*>, vararg args: TsTypeRef): TsTypeRef =
                TsTypeRef.Named(TypeId.declOf(cls, cls.createBareType()), args.toList())

            "a concrete argument" {
                refIn("nonNullArg") shouldBe named(FxBox::class, TsTypeRef.TsString)
            }

            "a NULLABLE argument is on the argument, not a separate declaration" {
                // Under monomorphization this needed a distinct name (FxBoxStringOrNull) and got it
                // wrong once — see 2623a08d. Generically it is just a nullable type argument.
                refIn("nullableArg") shouldBe named(FxBox::class, TsTypeRef.Nullable(TsTypeRef.TsString))
            }

            "a generic nested in a generic substitutes at BOTH levels" {
                refIn("twoDeep") shouldBe named(
                    FxPageOf::class,
                    named(FxBox::class, named(FxSpeaker::class)),
                )
            }

            "three levels deep" {
                refIn("threeDeep") shouldBe named(
                    FxPageOf::class,
                    named(FxBox::class, named(FxPageOf::class, named(FxSpeaker::class))),
                )
            }

            "a generic inside a list stays an array of references" {
                refIn("genericInList") shouldBe
                        TsTypeRef.ArrayOf(named(FxBox::class, named(FxSpeaker::class)))
            }

            "a Set is an array too" {
                refIn("genericInSet") shouldBe
                        TsTypeRef.ArrayOf(named(FxBox::class, named(FxStatus::class)))
            }

            "a generic inside a map value" {
                refIn("genericInMap") shouldBe
                        TsTypeRef.RecordOf(named(FxBox::class, named(FxTalkId::class)))
            }

            "a container AS the argument" {
                refIn("listInsideGeneric") shouldBe
                        named(FxBox::class, TsTypeRef.ArrayOf(named(FxSpeaker::class)))
            }

            "a map AS the argument" {
                refIn("mapInsideGeneric") shouldBe
                        named(FxBox::class, TsTypeRef.RecordOf(named(FxSpeaker::class)))
            }

            "a nested container as the argument" {
                refIn("listOfListInsideGeneric") shouldBe named(
                    FxBox::class,
                    TsTypeRef.ArrayOf(TsTypeRef.ArrayOf(named(FxSpeaker::class))),
                )
            }

            "a nullable generic INSTANTIATION" {
                refIn("nullableGeneric") shouldBe
                        TsTypeRef.Nullable(named(FxBox::class, named(FxSpeaker::class)))
            }

            "a list of nullable generic instantiations" {
                refIn("listOfNullableGeneric") shouldBe TsTypeRef.ArrayOf(
                    TsTypeRef.Nullable(named(FxBox::class, named(FxSpeaker::class)))
                )
            }

            "an enum, a value class and a data class all work as arguments" {
                refIn("enumArg") shouldBe named(FxBox::class, named(FxStatus::class))
                refIn("valueClassArg") shouldBe named(FxBox::class, named(FxTalkId::class))
                refIn("threeParams") shouldBe named(
                    FxTriple::class, named(FxSpeaker::class), named(FxStatus::class), named(FxTalkId::class),
                )
            }
        }

        "star-projected arguments are recorded, never silently dropped" - {

            // The container branches already handle `List<*>`; the USER-GENERIC path did not. Dropping
            // the position leaves the reference with fewer arguments than the declaration has
            // parameters, which emits either a bare factory (a runtime throw on first parse) or a call
            // with the wrong arity.

            "a fully star-projected generic" {
                val model = walk(typeOf<FxStarGeneric>())

                model.undetermined.map { it.reason }.firstOrNull() shouldNotBe null
                model.propOf(FxStarGeneric::class, "boxed")
                    .shouldBeInstanceOf<TsTypeRef.Named>()
                    .args.size shouldBe 1
            }

            "a partially star-projected generic keeps the arity" {
                val model = walk(typeOf<FxPartialStarGeneric>())

                withClue("3 parameters must mean 3 arguments, or the emitted call is malformed") {
                    model.propOf(FxPartialStarGeneric::class, "triple")
                        .shouldBeInstanceOf<TsTypeRef.Named>()
                        .args.size shouldBe 3
                }

                model.undetermined.size shouldBe 2
            }
        }

        "a variant's parameters are bound through its SUPERTYPE, not positionally" - {

            "a child that REORDERS its parent's parameters is bound correctly" {
                // FxLeft<R, L> : FxEither<L, R>. Passing the parent's [L, R] straight through swaps
                // them — and the emitted type and schema agree with each other, so `satisfies` holds,
                // tsc is clean, and the client then rejects every real payload.
                val union = walk(typeOf<FxHoldsEither>()).declOf(FxEither::class)
                    .shouldBeInstanceOf<TsTypeDecl.Union>()

                union.typeParams shouldContainExactly listOf("L", "R")

                val left = union.variants.first { it.id.key == FxLeft::class.qualifiedName }

                withClue("FxLeft declares <R, L>, so its arguments must arrive in that order") {
                    left.args shouldContainExactly listOf(
                        TsTypeRef.TypeParam("R"), TsTypeRef.TypeParam("L"),
                    )
                }
            }

            "a child that TRANSFORMS its parent's argument is reported, not guessed at" {
                // FxBatched<U> : FxFeed<List<U>> has matching arity but is not a pass-through, so the
                // old arity check accepted it and emitted a schema demanding one list level too many.
                val model = walk(typeOf<FxHoldsFeed>())

                model.undetermined.map { it.reason }.any { "one-to-one" in it } shouldBe true
            }

            "a child binding one parameter to BOTH of its parent's is reported" {
                // FxBoth<A> : FxSame<A, A>. Arity matches and every child parameter IS bound, so only
                // checking coverage lets it through with a last-wins map — emitting FxBoth<Y>.
                val model = walk(typeOf<FxHoldsSame>())

                model.undetermined.map { it.reason }.any { "one-to-one" in it } shouldBe true
            }

            "an ordinary pass-through hierarchy is unaffected" {
                val union = walk(typeOf<FxGenericMatrix>()).declOf(FxStorable::class)
                    .shouldBeInstanceOf<TsTypeDecl.Union>()

                union.variants.forEach { it.args shouldContainExactly listOf(TsTypeRef.TypeParam("T")) }
            }
        }

        "a recursive generic terminates and refers to itself with its own parameter" {
            val decl = walk(typeOf<FxGenericMatrix>()).objOf(FxTreeOf::class)

            decl.typeParams shouldContainExactly listOf("T")

            withClue("children: List<FxTreeOf<T>> — the self-reference must carry T, not an argument") {
                decl.props.first { it.name == "children" }.type shouldBe TsTypeRef.ArrayOf(
                    TsTypeRef.Named(
                        TypeId.declOf(FxTreeOf::class, FxTreeOf::class.createBareType()),
                        listOf(TsTypeRef.TypeParam("T")),
                    )
                )
            }
        }

        "a GENERIC sealed hierarchy keeps its payload type" - {

            // The defect monomorphization caused: every instantiation collapsed onto one type carrying
            // `unknown`, so Storable<Speaker> and Storable<TalkId> were indistinguishable — and the
            // schema still parsed, because `unknown` accepts anything.

            "the union declares the parameter and passes it to every variant" {
                val union = walk(typeOf<FxGenericMatrix>()).declOf(FxStorable::class)
                    .shouldBeInstanceOf<TsTypeDecl.Union>()

                union.typeParams shouldContainExactly listOf("T")

                withClue("each variant must receive the parent's parameter, not Any") {
                    union.variants.forEach { variant ->
                        variant.args shouldContainExactly listOf(TsTypeRef.TypeParam("T"))
                    }
                }
            }

            "the variants are declared generically too" {
                val model = walk(typeOf<FxGenericMatrix>())

                model.objOf(FxStorable.Stored::class).typeParams shouldContainExactly listOf("T")

                withClue("the payload is the parameter — under monomorphization it was `unknown`") {
                    model.propOf(FxStorable.Stored::class, "value") shouldBe TsTypeRef.TypeParam("T")
                }
            }

            "two instantiations of one hierarchy stay distinct at the use site" {
                val model = walk(typeOf<FxGenericMatrix>())

                val a = model.propOf(FxGenericMatrix::class, "storable")
                val b = model.propOf(FxGenericMatrix::class, "storableOther")

                withClue("these were the SAME type before generic emission") {
                    (a == b) shouldBe false
                }
            }
        }
    }
}
