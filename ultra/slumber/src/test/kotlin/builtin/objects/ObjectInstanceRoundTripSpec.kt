package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.serialization.SerialName

class ObjectInstanceRoundTripSpec : StringSpec() {

    object SingletonOne

    sealed class PolymorphicSingletons {
        @SerialName("one")
        object One : PolymorphicSingletons()

        @SerialName("two")
        object Two : PolymorphicSingletons()
    }

    init {

        "Slumbering and awaking a singleton object must work" {

            val codec = Codec.default

            val slumbered = codec.slumber(SingletonOne)

            slumbered shouldBe emptyMap<Any, Any>()

            val awoken = codec.awake(SingletonOne::class, slumbered)

            awoken shouldBeSameInstanceAs SingletonOne
        }

        "Slumbering and awaking a singleton object as part of a data class must work" {

            data class DataClass(val singleton: SingletonOne)

            val codec = Codec.default

            val data = DataClass(SingletonOne)

            val slumbered = codec.slumber(data)

            slumbered shouldBe mapOf(
                "singleton" to emptyMap<Any, Any>()
            )

            val awoken = codec.awake(DataClass::class, slumbered)

            awoken shouldBe data
        }

        "Slumbering and awaking a polymorphic singleton objects as part of a data class must work" {

            data class DataClass(val singletons: List<PolymorphicSingletons>)

            val codec = Codec.default

            val data = DataClass(
                listOf(
                    PolymorphicSingletons.One,
                    PolymorphicSingletons.Two
                )
            )

            val slumbered = codec.slumber(data)

            slumbered shouldBe mapOf(
                "singletons" to listOf(
                    mapOf("_type" to "one"),
                    mapOf("_type" to "two"),
                )

            )

            val awoken = codec.awake(DataClass::class, slumbered)

            awoken!! shouldBe data
            awoken.singletons[0] shouldBe PolymorphicSingletons.One
            awoken.singletons[1] shouldBe PolymorphicSingletons.Two
        }
    }
}
