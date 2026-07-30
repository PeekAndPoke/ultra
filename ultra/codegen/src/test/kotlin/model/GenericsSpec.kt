package io.peekandpoke.ultra.codegen.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Generic types are MONOMORPHIZED: each instantiation becomes its own declaration.
 *
 * The walker reifies type arguments anyway (via `ReifiedKType`), so staying generic would mean
 * *un*-reifying, and generic zod schemas require function-valued schemas that `z.infer` cannot see
 * through. Monomorphizing costs one declaration per instantiation and keeps the emitted schemas plain.
 */
class GenericsSpec : FreeSpec() {

    private fun walk(vararg types: KType): TypeModel =
        TypeWalker(TsTypeClaims()).walk(types.map { TypeWalker.Root(it, "root") })

    private fun TypeModel.names(): List<String> = decls.values.map { it.name }

    private fun TypeModel.propTypeOf(owner: KType, prop: String): TsTypeRef =
        (decls[TypeId.of(owner)] as TsTypeDecl.Obj).props.first { it.name == prop }.type

    init {
        "every generic position resolves — nothing is left unclassified" {
            walk(typeOf<FxGenericHolder>()).unresolved shouldContainExactly emptyList()
        }

        "a nullable type argument is a different instantiation from a non-nullable one" - {
            // Both halves matter. Sharing a TypeId gives the second property the first one's schema,
            // so a server sending `{"item": null}` is rejected by a schema that says `z.string()`.
            // Sharing a NAME is a different failure: two declarations compete for one const.

            "the two instantiations get separate declarations" {
                val model = walk(typeOf<FxNullableArgs>())

                withClue("distinct TypeIds") {
                    TypeId.of(typeOf<FxBox<String>>()) shouldNotBe TypeId.of(typeOf<FxBox<String?>>())
                }

                model.decls.keys shouldContain TypeId.of(typeOf<FxBox<String>>())
                model.decls.keys shouldContain TypeId.of(typeOf<FxBox<String?>>())
            }

            "each declaration carries the nullability of its own argument" {
                val model = walk(typeOf<FxNullableArgs>())

                fun itemTypeOf(owner: KType): TsTypeRef = model.propTypeOf(owner, "item")

                itemTypeOf(typeOf<FxBox<String>>()) shouldBe TsTypeRef.TsString
                itemTypeOf(typeOf<FxBox<String?>>()) shouldBe TsTypeRef.Nullable(TsTypeRef.TsString)
            }

            "the two declarations do not compete for one TypeScript name" {
                val model = walk(typeOf<FxNullableArgs>())

                val names = model.names()

                withClue("emitted names: $names") {
                    names.size shouldBe names.distinct().size
                }
            }
        }

        "a generic as a property is monomorphized" {
            val model = walk(typeOf<FxGenericHolder>())

            model.propTypeOf(typeOf<FxGenericHolder>(), "page") shouldBe
                    TsTypeRef.Named(TypeId.of(typeOf<FxPageOf<FxSpeaker>>()))

            model.decls[TypeId.of(typeOf<FxPageOf<FxSpeaker>>())]!!.name shouldBe "FxPageOfFxSpeaker"
        }

        "a generic inside a list is monomorphized and the list stays an array" {
            walk(typeOf<FxGenericHolder>()).propTypeOf(typeOf<FxGenericHolder>(), "boxes") shouldBe
                    TsTypeRef.ArrayOf(TsTypeRef.Named(TypeId.of(typeOf<FxBox<FxSpeaker>>())))
        }

        "a generic inside a map value is monomorphized" {
            walk(typeOf<FxGenericHolder>()).propTypeOf(typeOf<FxGenericHolder>(), "mapped") shouldBe
                    TsTypeRef.RecordOf(TsTypeRef.Named(TypeId.of(typeOf<FxBox<FxTalkId>>())))
        }

        "a collection AS the type argument keeps a readable name" {
            val model = walk(typeOf<FxGenericHolder>())

            withClue("List::class.java is java.util.List while its qualifiedName is kotlin.collections.List") {
                model.decls[TypeId.of(typeOf<FxBox<List<FxSpeaker>>>())]!!.name shouldBe "FxBoxListFxSpeaker"
            }
        }

        "a generic nested in a generic is monomorphized at both levels" {
            val model = walk(typeOf<FxGenericHolder>())

            val deep = model.decls[TypeId.of(typeOf<FxPageOf<FxBox<FxSpeaker>>>())]
                .shouldBeInstanceOf<TsTypeDecl.Obj>()

            deep.name shouldBe "FxPageOfFxBoxFxSpeaker"

            withClue("the inner type argument must be substituted at the inner level too") {
                deep.props.first { it.name == "items" }.type shouldBe
                        TsTypeRef.ArrayOf(TsTypeRef.Named(TypeId.of(typeOf<FxBox<FxSpeaker>>())))
            }
        }

        "a generic with two type parameters concatenates both" {
            val model = walk(typeOf<FxGenericHolder>())

            val pair = model.decls[TypeId.of(typeOf<FxPair<FxSpeaker, FxStatus>>())]
                .shouldBeInstanceOf<TsTypeDecl.Obj>()

            pair.name shouldBe "FxPairFxSpeakerFxStatus"
            pair.props.map { it.name } shouldContainExactly listOf("first", "second")
        }

        "the same generic at two instantiations produces two declarations" {
            val model = walk(typeOf<FxTwoInstantiations>())

            withClue("one shared declaration would have to be generic, which z.infer cannot see through") {
                model.names() shouldContainExactlyInAnyOrder listOf(
                    "FxSpeaker", "FxBoxFxSpeaker", "FxStatus", "FxBoxFxStatus", "FxTwoInstantiations",
                )
            }
        }

        "a generic reached only as a root is still declared" {
            walk(typeOf<FxPageOf<FxTalk>>()).decls[TypeId.of(typeOf<FxPageOf<FxTalk>>())]
                .shouldBeInstanceOf<TsTypeDecl.Obj>()
                .name shouldBe "FxPageOfFxTalk"
        }
    }
}
