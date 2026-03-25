package io.peekandpoke.ultra.slumber

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

class SlumberHelpersSpec : StringSpec({

    // Simple stub serializer for testing — no real serialization needed
    fun <T : Any> stubSerializer(name: String): KSerializer<T> = object : KSerializer<T> {
        override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder): T = error("stub")
        override fun serialize(encoder: Encoder, value: T) = error("stub")
    }

    open class Base
    class ChildA : Base()
    class ChildB : Base()
    class Unrelated

    "PolymorphicChildrenToSerializers built manually has correct entries" {
        val entries = listOf(
            PolymorphicChildrenToSerializers.TypeAndSerializer(ChildA::class, stubSerializer<ChildA>("A")),
            PolymorphicChildrenToSerializers.TypeAndSerializer(ChildB::class, stubSerializer<ChildB>("B")),
        )

        val c2s = PolymorphicChildrenToSerializers(Base::class, entries)

        c2s.base shouldBe Base::class
        c2s.entries.size shouldBe 2
        c2s.entries[0].type shouldBe ChildA::class
        c2s.entries[1].type shouldBe ChildB::class
    }

    "PolymorphicChildrenToSerializers implements Set correctly" {
        val entries = listOf(
            PolymorphicChildrenToSerializers.TypeAndSerializer(ChildA::class, stubSerializer<ChildA>("A")),
            PolymorphicChildrenToSerializers.TypeAndSerializer(ChildB::class, stubSerializer<ChildB>("B")),
        )

        val c2s = PolymorphicChildrenToSerializers(Base::class, entries)

        c2s.size shouldBe 2
        c2s.isEmpty() shouldBe false
        c2s.contains(ChildA::class) shouldBe true
        c2s.contains(ChildB::class) shouldBe true
        @Suppress("UNCHECKED_CAST")
        c2s.contains(Unrelated::class as KClass<out Base>) shouldBe false
        c2s.containsAll(listOf(ChildA::class, ChildB::class)) shouldBe true
        c2s.iterator().asSequence().toSet() shouldBe setOf(ChildA::class, ChildB::class)
    }

    "Empty PolymorphicChildrenToSerializers" {
        val c2s = PolymorphicChildrenToSerializers<Base>(Base::class, emptyList())

        c2s.size shouldBe 0
        c2s.isEmpty() shouldBe true
    }

    "addAll merges nested entries" {
        val first = PolymorphicChildrenToSerializers(
            Base::class,
            listOf(PolymorphicChildrenToSerializers.TypeAndSerializer(ChildA::class, stubSerializer<ChildA>("A")))
        )

        val builder = PolymorphicChildrenToSerializers.Builder(Base::class)
        builder.entries.add(
            PolymorphicChildrenToSerializers.TypeAndSerializer(
                ChildB::class,
                stubSerializer<ChildB>("B")
            )
        )
        builder.addAll(first)
        val combined = builder.build()

        combined.size shouldBe 2
        combined.contains(ChildA::class) shouldBe true
        combined.contains(ChildB::class) shouldBe true
    }
})
