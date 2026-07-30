package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.peekandpoke.ultra.codegen.model.FxEvent
import io.peekandpoke.ultra.codegen.model.FxNode
import io.peekandpoke.ultra.codegen.model.FxResult
import io.peekandpoke.ultra.codegen.model.FxSeatCount
import io.peekandpoke.ultra.codegen.model.FxShape
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxStatus
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.FxTalkId
import io.peekandpoke.ultra.codegen.model.FxWithExtraField
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TsTypeDecl
import io.peekandpoke.ultra.codegen.model.TypeId
import io.peekandpoke.ultra.codegen.model.TypeWalker
import io.peekandpoke.ultra.slumber.Codec
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Asserts that the fields the generator emits are EXACTLY the keys Slumber writes.
 *
 * This is the property the whole design rests on, checked against the real serializer rather than
 * against the walker's own assumptions. It is the pure-Kotlin half of the round trip; the other half
 * (the emitted zod schema parsing that JSON) is verified with node — see the task doc.
 */
class SlumberFieldParitySpec : FreeSpec() {

    private val codec = Codec.default

    private fun keysEmittedFor(type: KType, instanceType: KType = type): Set<String> {
        val model = TypeWalker(TsTypeClaims()).walk(listOf(TypeWalker.Root(type, "root")))

        val decl = model.decls[TypeId.of(instanceType)] as? TsTypeDecl.Obj
            ?: error("no object declaration emitted for $instanceType")

        return decl.props.map { it.name }.toSet() + listOfNotNull(decl.discriminator?.field)
    }

    private fun keysSlumberedFor(type: KType, value: Any?): Set<String> {
        val slumbered = codec.slumber(type, value)

        @Suppress("UNCHECKED_CAST")
        return (slumbered as Map<String, Any?>).keys
    }

    private fun checkParity(type: KType, value: Any?, instanceType: KType = type) {
        withClue("emitted fields must match the keys Slumber actually writes for $instanceType") {
            keysEmittedFor(type, instanceType) shouldContainExactlyInAnyOrder keysSlumberedFor(type, value)
        }
    }

    init {
        "a data class with every container shape" {
            checkParity(
                typeOf<FxTalk>(),
                FxTalk(
                    id = FxTalkId("t-1"),
                    title = "Hello",
                    status = FxStatus.ACTIVE,
                    speakers = listOf(FxSpeaker("Ada", null)),
                    tags = setOf("a"),
                    meta = mapOf("k" to "v"),
                    seats = FxSeatCount(42),
                    durationMs = 1234L,
                    rating = null,
                    featured = true,
                ),
            )
        }

        "a @Slumber.Field property outside the constructor" {
            checkParity(typeOf<FxWithExtraField>(), FxWithExtraField("a"))
        }

        "a polymorphic child carries its discriminator" {
            checkParity(typeOf<FxShape>(), FxShape.Circle(1.0), instanceType = typeOf<FxShape.Circle>())
        }

        "a polymorphic child with a custom discriminator field" {
            checkParity(typeOf<FxEvent>(), FxEvent.Created("now"), instanceType = typeOf<FxEvent.Created>())
        }

        "a plain sealed object variant slumbers to just its discriminator" {
            checkParity(typeOf<FxResult>(), FxResult.Pending, instanceType = typeOf<FxResult.Pending>())
        }

        "a recursive type" {
            checkParity(typeOf<FxNode>(), FxNode("root", emptyList(), null))
        }
    }
}
