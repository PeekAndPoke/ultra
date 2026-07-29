package io.peekandpoke.ultra.codegen.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class TypeWalkerSpec : FreeSpec() {

    private fun walkOf(type: KType, claims: TsTypeClaims = TsTypeClaims()): TypeModel =
        TypeWalker(claims).walk(listOf(TypeWalker.Root(type, "root")))

    private fun TypeModel.declFor(type: KType): TsTypeDecl? = decls[TypeId.of(type)]

    init {
        "TypeId" - {

            "strips nullability so T and T? share one declaration" {
                TypeId.of(typeOf<FxSpeaker>()) shouldBe TypeId.of(typeOf<FxSpeaker?>())
            }

            "distinguishes generic instantiations" {
                val a = TypeId.of(typeOf<FxPageOf<FxTalk>>())
                val b = TypeId.of(typeOf<FxPageOf<FxSpeaker>>())

                withClue("PageOf<Talk> and PageOf<Speaker> are different declarations") {
                    (a == b) shouldBe false
                }
            }

            "is equal across KType instances built different ways" {
                val fromTypeOf = TypeId.of(typeOf<FxSpeaker>())
                val fromCreateType = TypeId.of(FxSpeaker::class.createBareType())

                withClue("typeOf<T>() and KClass.createType() must agree — the walker mixes both") {
                    fromTypeOf shouldBe fromCreateType
                }
            }
        }

        "data classes" - {

            "maps primitives, collections and nullability" {
                val model = walkOf(typeOf<FxTalk>())

                val talk = model.declFor(typeOf<FxTalk>()).shouldBeInstanceOf<TsTypeDecl.Obj>()

                val byName = talk.props.associateBy { it.name }

                byName.getValue("title").type shouldBe TsTypeRef.TsString
                byName.getValue("speakers").type shouldBe
                        TsTypeRef.ArrayOf(TsTypeRef.Named(TypeId.of(typeOf<FxSpeaker>())))
                byName.getValue("tags").type shouldBe TsTypeRef.ArrayOf(TsTypeRef.TsString)
                byName.getValue("meta").type shouldBe TsTypeRef.RecordOf(TsTypeRef.TsString)
                byName.getValue("rating").type shouldBe TsTypeRef.Nullable(TsTypeRef.TsNumber)
            }

            "a Set maps to an array, matching the JSON Slumber writes" {
                val model = walkOf(typeOf<FxTalk>())
                val talk = model.declFor(typeOf<FxTalk>()).shouldBeInstanceOf<TsTypeDecl.Obj>()

                talk.props.first { it.name == "tags" }.type shouldBe TsTypeRef.ArrayOf(TsTypeRef.TsString)
            }

            "a constructor default makes the property optional" {
                val model = walkOf(typeOf<FxTalk>())
                val talk = model.declFor(typeOf<FxTalk>()).shouldBeInstanceOf<TsTypeDecl.Obj>()

                talk.props.first { it.name == "featured" }.optional shouldBe true
                talk.props.first { it.name == "title" }.optional shouldBe false
            }

            "uses the property name as the wire name, unmodified" {
                val model = walkOf(typeOf<FxSpeaker>())
                val speaker = model.declFor(typeOf<FxSpeaker>()).shouldBeInstanceOf<TsTypeDecl.Obj>()

                speaker.props.map { it.name } shouldContainExactly listOf("name", "bio")
            }

            "includes a non-constructor property carrying @Slumber.Field" {
                val model = walkOf(typeOf<FxWithExtraField>())
                val decl = model.declFor(typeOf<FxWithExtraField>()).shouldBeInstanceOf<TsTypeDecl.Obj>()

                withClue("DataClassSlumberer serializes @Slumber.Field properties, so they must be typed") {
                    decl.props.map { it.name } shouldContainExactlyInAnyOrder listOf("a", "computed")
                }
            }
        }

        "enums" - {

            "emit the constant names, which is what EnumCodec writes" {
                val model = walkOf(typeOf<FxStatus>())
                val decl = model.declFor(typeOf<FxStatus>()).shouldBeInstanceOf<TsTypeDecl.EnumDecl>()

                decl.values shouldContainExactly listOf("ACTIVE", "ARCHIVED")
            }
        }

        "value classes" - {

            "alias to the underlying scalar, never an object" {
                val model = walkOf(typeOf<FxTalk>())

                model.declFor(typeOf<FxTalkId>())
                    .shouldBeInstanceOf<TsTypeDecl.Alias>()
                    .target shouldBe TsTypeRef.TsString

                model.declFor(typeOf<FxSeatCount>())
                    .shouldBeInstanceOf<TsTypeDecl.Alias>()
                    .target shouldBe TsTypeRef.TsNumber
            }
        }

        "polymorphism" - {

            "a sealed class becomes a discriminated union over its children" {
                val model = walkOf(typeOf<FxShape>())

                val union = model.declFor(typeOf<FxShape>()).shouldBeInstanceOf<TsTypeDecl.Union>()

                union.discriminatorField shouldBe "_type"
                union.variants shouldContainExactlyInAnyOrder listOf(
                    TypeId.of(FxShape.Circle::class.createBareType()),
                    TypeId.of(FxShape.Square::class.createBareType()),
                )
            }

            "children are declared and carry the discriminator literal" {
                val model = walkOf(typeOf<FxShape>())

                val circle = model.declFor(FxShape.Circle::class.createBareType())
                    .shouldBeInstanceOf<TsTypeDecl.Obj>()

                circle.discriminator.shouldNotBeNull().field shouldBe "_type"
                circle.discriminator.shouldNotBeNull().literal shouldBe FxShape.Circle::class.qualifiedName
            }

            "honours a custom discriminator from a Polymorphic.Parent companion" {
                val model = walkOf(typeOf<FxEvent>())

                val union = model.declFor(typeOf<FxEvent>()).shouldBeInstanceOf<TsTypeDecl.Union>()

                withClue("the discriminator is NOT always _type — a parent companion can override it") {
                    union.discriminatorField shouldBe "kind"
                }
            }

            "honours Polymorphic.Child.identifier and @SerialName" {
                val model = walkOf(typeOf<FxEvent>())

                model.declFor(FxEvent.Created::class.createBareType())
                    .shouldBeInstanceOf<TsTypeDecl.Obj>()
                    .discriminator.shouldNotBeNull().literal shouldBe "created"

                model.declFor(FxEvent.Deleted::class.createBareType())
                    .shouldBeInstanceOf<TsTypeDecl.Obj>()
                    .discriminator.shouldNotBeNull().literal shouldBe "deleted"
            }
        }

        "cycles" - {

            "a self-referential type terminates" {
                val model = walkOf(typeOf<FxNode>())

                val node = model.declFor(typeOf<FxNode>()).shouldBeInstanceOf<TsTypeDecl.Obj>()

                node.props.first { it.name == "children" }.type shouldBe
                        TsTypeRef.ArrayOf(TsTypeRef.Named(TypeId.of(typeOf<FxNode>())))
                node.props.first { it.name == "parent" }.type shouldBe
                        TsTypeRef.Nullable(TsTypeRef.Named(TypeId.of(typeOf<FxNode>())))
            }

            "mutually recursive types both get declared" {
                val model = walkOf(typeOf<FxMutualA>())

                model.declFor(typeOf<FxMutualA>()).shouldNotBeNull()
                model.declFor(typeOf<FxMutualB>()).shouldNotBeNull()
            }
        }

        "generics" - {

            "monomorphize per instantiation" {
                val model = TypeWalker(TsTypeClaims()).walk(
                    listOf(
                        TypeWalker.Root(typeOf<FxPageOf<FxTalk>>(), "a"),
                        TypeWalker.Root(typeOf<FxPageOf<FxSpeaker>>(), "b"),
                    )
                )

                val talkPage = model.declFor(typeOf<FxPageOf<FxTalk>>()).shouldBeInstanceOf<TsTypeDecl.Obj>()
                val speakerPage = model.declFor(typeOf<FxPageOf<FxSpeaker>>()).shouldBeInstanceOf<TsTypeDecl.Obj>()

                talkPage.name shouldBe "FxPageOfFxTalk"
                speakerPage.name shouldBe "FxPageOfFxSpeaker"

                withClue("the type parameter must be substituted, not left abstract") {
                    talkPage.props.first { it.name == "items" }.type shouldBe
                            TsTypeRef.ArrayOf(TsTypeRef.Named(TypeId.of(typeOf<FxTalk>())))
                }
            }
        }

        "claims" - {

            "a claimed type is referenced but never declared, and is not walked into" {
                val claims = TsTypeClaims().apply {
                    scopeFor("test").map<FxCustomCodecType>(tsName = "Stamp", importFrom = "./runtime")
                }

                val model = walkOf(typeOf<FxHoldsClaimed>(), claims)

                withClue("a custom codec reshapes the JSON — the declared structure must be ignored") {
                    model.declFor(FxCustomCodecType::class.createBareType()) shouldBe null
                }

                model.usedClaims.keys shouldContainExactly listOf(FxCustomCodecType::class.qualifiedName)

                model.declFor(typeOf<FxHoldsClaimed>())
                    .shouldBeInstanceOf<TsTypeDecl.Obj>()
                    .props.first { it.name == "stamp" }.type shouldBe
                        TsTypeRef.Named(TypeId.of(FxCustomCodecType::class.createBareType()))
            }

            "an opaque claim resolves to unknown" {
                val claims = TsTypeClaims().apply {
                    scopeFor("test").opaque<FxCustomCodecType>(reason = "internal")
                }

                val model = walkOf(typeOf<FxHoldsClaimed>(), claims)

                model.declFor(typeOf<FxHoldsClaimed>())
                    .shouldBeInstanceOf<TsTypeDecl.Obj>()
                    .props.first { it.name == "stamp" }.type shouldBe TsTypeRef.TsUnknown
            }

            "a claimed polymorphic CHILD is still not declared" {
                val claims = TsTypeClaims().apply {
                    scopeFor("test").map<FxPartlyClaimed.Custom>(tsName = "CustomVariant", importFrom = "./runtime")
                }

                val model = walkOf(typeOf<FxPartlyClaimed>(), claims)

                withClue("union variants are enqueued directly, bypassing reference resolution") {
                    model.declFor(FxPartlyClaimed.Custom::class.createBareType()) shouldBe null
                }

                withClue("the unclaimed sibling must still be declared") {
                    model.declFor(FxPartlyClaimed.Plain::class.createBareType()).shouldNotBeNull()
                }

                withClue("the variant stays listed on the union — it is referenced, just not declared") {
                    model.declFor(typeOf<FxPartlyClaimed>())
                        .shouldBeInstanceOf<TsTypeDecl.Union>()
                        .variants shouldContainExactlyInAnyOrder listOf(
                        TypeId.of(FxPartlyClaimed.Plain::class.createBareType()),
                        TypeId.of(FxPartlyClaimed.Custom::class.createBareType()),
                    )
                }
            }

            "only claims that were actually reached are recorded" {
                val claims = TsTypeClaims().apply {
                    scopeFor("test").map<FxCustomCodecType>(tsName = "Stamp")
                }

                val model = walkOf(typeOf<FxSpeaker>(), claims)

                withClue("an unreachable claim must not pull dead runtime code into the SDK") {
                    model.usedClaims.keys shouldContainExactly emptyList()
                }
            }
        }

        "parity with BuiltInModule.getSlumberer" - {

            "a PLAIN sealed object variant is an empty object, not reported unresolved" {
                val model = walkOf(typeOf<FxResult>())

                withClue("a plain object is not isData — only the ObjectInstanceCodec branch types it") {
                    val obj = model.declFor(FxResult.Pending::class.createBareType())
                        .shouldBeInstanceOf<TsTypeDecl.Obj>()

                    obj.props shouldContainExactly emptyList()
                    obj.discriminator.shouldNotBeNull().field shouldBe "_type"
                }

                model.unresolved shouldContainExactly emptyList()
            }

            "a `data object` sealed variant is also an empty object" {
                val model = walkOf(typeOf<FxResult>())

                model.declFor(FxResult.Skipped::class.createBareType())
                    .shouldBeInstanceOf<TsTypeDecl.Obj>()
                    .props shouldContainExactly emptyList()
            }

            "a non-data class with a no-arg constructor is typed, since Slumber routes it to DataClassSlumberer" {
                val model = walkOf(typeOf<FxHoldsNoArgCtor>())

                model.declFor(FxNoArgCtor::class.createBareType()).shouldBeInstanceOf<TsTypeDecl.Obj>()
                model.unresolved shouldContainExactly emptyList()
            }

            "a value class is resolved BEFORE collections, so a List-wrapping value class aliases" {
                val model = walkOf(typeOf<FxHoldsIdList>())

                withClue("Slumber checks isUserValueClass before the Iterable branch") {
                    model.declFor(FxIdList::class.createBareType())
                        .shouldBeInstanceOf<TsTypeDecl.Alias>()
                        .target shouldBe TsTypeRef.ArrayOf(TsTypeRef.TsString)
                }
            }

            "a kotlin stdlib value class is unresolved, matching Slumber's refusal" {
                val model = walkOf(typeOf<FxHoldsStdlibValueClass>())

                withClue("Slumber excludes kotlin.* value classes — emitting an alias would type something the server cannot write") {
                    model.unresolved.map { it.id.cls } shouldContainExactly listOf(UInt::class)
                }
            }

            "an array is unresolved, because Array is not Iterable and Slumber cannot slumber it" {
                val model = walkOf(typeOf<FxHoldsArray>())

                model.unresolved.size shouldBe 1
                model.unresolved.first().reason.contains("Array is not Iterable") shouldBe true
            }
        }

        "unresolved types" - {

            "a plain interface is reported with the path that reached it" {
                val model = walkOf(typeOf<FxHoldsInterface>())

                model.unresolved.size shouldBe 1

                val found = model.unresolved.first()

                found.id shouldBe TypeId.of(FxPlainInterface::class.createBareType())
                found.path shouldContainExactly listOf("root", "thing")
            }
        }

        "advisories" - {

            "a Long is flagged, since JSON.parse truncates above 2^53" {
                val model = walkOf(typeOf<FxTalk>())

                model.longValued.map { it.path } shouldContainExactly listOf(listOf("root", "durationMs"))
            }
        }

        "naming" - {

            "nested classes keep their outer prefix so simple-name clashes do not collide" {
                val model = walkOf(typeOf<FxHoldsBothInners>())

                val names = model.decls.values.map { it.name }

                withClue("two nested 'Inner' classes in one package must not produce the same TS name") {
                    names shouldContainExactlyInAnyOrder listOf(
                        "FxHoldsBothInners", "FxOuterAInner", "FxOuterBInner",
                    )
                }
            }
        }
    }
}
