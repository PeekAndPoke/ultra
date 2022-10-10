package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.Slumber
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AnnotatedFieldsSlumberSpec : StringSpec() {

    @Suppress("unused")
    data class DataClassWithAnnotatedFields(
        val num: Int,
        val factor: Int,
    ) {
        @Slumber.Field
        val field = num * factor

        @Slumber.Field
        val getter get() = num * factor

        @Slumber.Field
        val lazy by lazy { num * factor }

        val notIncluded = 10
    }

    @Suppress("unused")
    data class GenericDataClassWithFields<T>(
        val input: T,
    ) {
        @Slumber.Field
        val field = input

        @Slumber.Field
        val getter get() = input

        @Slumber.Field
        val lazy by lazy { input }

        val notIncluded = 10
    }

    interface InterfaceWithFieldOne<T> {
        val input: T

        @Slumber.Field
        val fieldOne: T get() = input
    }

    interface InterfaceWithFieldTwo<T> {
        val input: T

        @Slumber.Field
        val fieldTwo: T get() = input
    }

    data class DataClassImplementingInterface(
        override val input: Int,
    ) : InterfaceWithFieldOne<Int>, InterfaceWithFieldTwo<Int>

    abstract class AbstractClassWithField<T> {
        abstract val input: T

        @Slumber.Field
        val field: T get() = input
    }

    data class DataClassExtendingAbstractClass(
        override val input: Int,
    ) : AbstractClassWithField<Int>()

    @Slumber.Field
    annotation class CustomAnnotation

    data class DataClassWithCustomAnnotation(
        val input: Int,
    ) {
        @CustomAnnotation
        val field: Int get() = input
    }

    init {

        "Slumbering a data class with @Slumber.Field annotated fields must work" {
            val input = DataClassWithAnnotatedFields(
                num = 10,
                factor = 5,
            )

            withClue("preventing 'notIncluded' from being optimized") {
                input.notIncluded shouldBe 10
            }

            val slumbered = Codec.default.slumber(input) as Map<*, *>
            withClue("size must be correct") {
                slumbered.size shouldBe 5
            }

            withClue("content must be correct") {
                slumbered shouldBe mapOf(
                    "num" to input.num,
                    "factor" to input.factor,
                    "field" to input.field,
                    "getter" to input.getter,
                    "lazy" to input.lazy,
                )
            }
        }

        "Slumbering a generic data class with @Slumber.Field annotated fields must work" {
            (Codec.default.slumber(GenericDataClassWithFields(input = 10)) as Map<*, *>).let {
                withClue("size must be correct") {
                    it.size shouldBe 4
                }

                withClue("content must be correct") {
                    it shouldBe mapOf(
                        "input" to 10,
                        "field" to 10,
                        "getter" to 10,
                        "lazy" to 10,
                    )
                }

            }

            (Codec.default.slumber(GenericDataClassWithFields(input = "10")) as Map<*, *>).let {
                withClue("size must be correct") {
                    it.size shouldBe 4
                }

                withClue("content must be correct") {
                    it shouldBe mapOf(
                        "input" to "10",
                        "field" to "10",
                        "getter" to "10",
                        "lazy" to "10",
                    )
                }
            }
        }

        "Slumbering a data class with @Slumber.Field annotated fields in super interface must work" {
            (Codec.default.slumber(DataClassImplementingInterface(input = 10)) as Map<*, *>).let {
                withClue("size must be correct") {
                    it.size shouldBe 3
                }

                withClue("content must be correct") {
                    it shouldBe mapOf(
                        "input" to 10,
                        "fieldOne" to 10,
                        "fieldTwo" to 10,
                    )
                }
            }
        }

        "Slumbering a data class with @Slumber.Field annotated fields in super class must work" {
            (Codec.default.slumber(DataClassExtendingAbstractClass(input = 10)) as Map<*, *>).let {
                withClue("size must be correct") {
                    it.size shouldBe 2
                }

                withClue("content must be correct") {
                    it shouldBe mapOf(
                        "input" to 10,
                        "field" to 10,
                    )
                }
            }
        }

        "Slumbering a data class with custom annotation that is itself annotated with @Slumber.Field" {
            (Codec.default.slumber(DataClassWithCustomAnnotation(input = 10)) as Map<*, *>).let {
                withClue("size must be correct") {
                    it.size shouldBe 2
                }

                withClue("content must be correct") {
                    it shouldBe mapOf(
                        "input" to 10,
                        "field" to 10,
                    )
                }
            }
        }
    }
}
