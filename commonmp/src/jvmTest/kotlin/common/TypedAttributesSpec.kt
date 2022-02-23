package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TypedAttributesSpec : FreeSpec() {

    init {
        "Getting an existing attribute must work" {

            val value1 = LocalDateTime.now()
            val value2 = ZonedDateTime.now()

            val key1 = TypedKey<LocalDateTime>("local")
            val key2 = TypedKey<ZonedDateTime>("zoned")
            val key3 = TypedKey<ZonedDateTime>("XXX")

            val subject = TypedAttributes {
                add(key1, value1)
                add(key2, value2)
            }

            assertSoftly {
                subject.size shouldBe 2

                subject[key1] shouldBe value1
                subject[key2] shouldBe value2
                subject[key3] shouldBe null
            }
        }

        "Conversion" - {
            "asMutable() must work properly" {
                val value1 = LocalDateTime.now()
                val value2 = ZonedDateTime.now()

                val key1 = TypedKey<LocalDateTime>("local")
                val key2 = TypedKey<ZonedDateTime>("zoned")

                val subject = TypedAttributes {
                    add(key1, value1)
                    add(key2, value2)
                }

                val mutable = subject.asMutable()

                subject.size shouldBe mutable.size
                subject.entries shouldBe mutable.entries
            }
        }

        "Adding elements" - {

            "using plus(other: TypedAttributes)" {
                val key1 = TypedKey<Int>("k1")
                val key2 = TypedKey<Int>("k2")
                val key3 = TypedKey<Int>("k3")

                val step01 = TypedAttributes.empty

                // Adding nothing
                val step02 = step01.plus(TypedAttributes.empty)

                step01 shouldBe step02

                // Adding two elements
                val step03 = step02.plus(TypedAttributes {
                    add(key1, 1)
                    add(key2, 2)
                })

                step03 shouldNotBe step02
                step03.entries shouldBe mapOf(key1 to 1, key2 to 2)

                // Overriding one element and adding another element
                val step04 = step03.plus(TypedAttributes {
                    add(key1, 10)
                    add(key3, 3)
                })

                step04 shouldNotBe step03

                step04.entries shouldBe mapOf(key1 to 10, key2 to 2, key3 to 3)
            }

            "using plus(builder)" {

                val key1 = TypedKey<Int>("k1")
                val key2 = TypedKey<Int>("k2")
                val key3 = TypedKey<Int>("k3")

                val step01 = TypedAttributes.empty

                // Adding nothing
                val step02 = step01.plus { }

                step01 shouldBe step02

                // Adding two elements
                val step03 = step02.plus {
                    add(key1, 1)
                    add(key2, 2)
                }

                step03 shouldNotBe step02
                step03.entries shouldBe mapOf(key1 to 1, key2 to 2)

                // Overriding one element and adding another element
                val step04 = step03.plus {
                    add(key1, 10)
                    add(key3, 3)
                }

                step04 shouldNotBe step03

                step04.entries shouldBe mapOf(key1 to 10, key2 to 2, key3 to 3)
            }
        }
    }
}
