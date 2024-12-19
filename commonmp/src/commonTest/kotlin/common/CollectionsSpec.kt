package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CollectionsSpec : StringSpec({

    "Collection.containsAny: vararg" {

        assertSoftly {

            listOf<Any>().containsAny() shouldBe false
            listOf<Any>().containsAny(1) shouldBe false
            listOf<Any>(1).containsAny() shouldBe false
            listOf<Any>(1).containsAny(2, 3) shouldBe false

            listOf<Any>(1).containsAny(1) shouldBe true
            listOf<Any>(1).containsAny(1, 2) shouldBe true
            listOf<Any>(1).containsAny(2, 1) shouldBe true
            listOf<Any>(1, 2, 3).containsAny(3, 4, 5) shouldBe true
        }
    }

    "Collection.containsNone: vararg" {

        assertSoftly {

            listOf<Any>().containsNone() shouldBe true
            listOf<Any>().containsNone(1) shouldBe true
            listOf<Any>(1).containsNone() shouldBe true
            listOf<Any>(1).containsNone(2, 3) shouldBe true

            listOf<Any>(1).containsNone(1) shouldBe false
            listOf<Any>(1).containsNone(1, 2) shouldBe false
            listOf<Any>(1).containsNone(2, 1) shouldBe false
            listOf<Any>(1, 2, 3).containsNone(3, 4, 5) shouldBe false
        }
    }

    "Collection.containsAny: array" {

        assertSoftly {

            listOf<Any>().containsAny(arrayOf<Any>()) shouldBe false
            listOf<Any>().containsAny(arrayOf(1)) shouldBe false
            listOf<Any>(1).containsAny(arrayOf<Any>()) shouldBe false
            listOf<Any>(1).containsAny(arrayOf(2, 3)) shouldBe false

            listOf<Any>(1).containsAny(arrayOf(1)) shouldBe true
            listOf<Any>(1).containsAny(arrayOf(1, 2)) shouldBe true
            listOf<Any>(1).containsAny(arrayOf(2, 1)) shouldBe true
            listOf<Any>(1, 2, 3).containsAny(arrayOf(3, 4, 5)) shouldBe true
        }
    }

    "Collection.containsNone: array" {

        assertSoftly {

            listOf<Any>().containsNone(arrayOf<Any>()) shouldBe true
            listOf<Any>().containsNone(arrayOf(1)) shouldBe true
            listOf<Any>(1).containsNone(arrayOf<Any>()) shouldBe true
            listOf<Any>(1).containsNone(arrayOf(2, 3)) shouldBe true

            listOf<Any>(1).containsNone(arrayOf(1)) shouldBe false
            listOf<Any>(1).containsNone(arrayOf(1, 2)) shouldBe false
            listOf<Any>(1).containsNone(arrayOf(2, 1)) shouldBe false
            listOf<Any>(1, 2, 3).containsNone(arrayOf(3, 4, 5)) shouldBe false
        }
    }

    "Collection.containsAny: collection" {

        assertSoftly {

            listOf<Any>().containsAny(listOf()) shouldBe false
            listOf<Any>().containsAny(listOf(1)) shouldBe false
            listOf<Any>(1).containsAny(listOf()) shouldBe false
            listOf<Any>(1).containsAny(listOf(2, 3)) shouldBe false

            listOf<Any>(1).containsAny(listOf(1)) shouldBe true
            listOf<Any>(1).containsAny(listOf(1, 2)) shouldBe true
            listOf<Any>(1).containsAny(listOf(2, 1)) shouldBe true
            listOf<Any>(1, 2, 3).containsAny(listOf(3, 4, 5)) shouldBe true
        }
    }

    "Collection.containsNone: collection" {

        assertSoftly {

            listOf<Any>().containsNone(listOf()) shouldBe true
            listOf<Any>().containsNone(listOf(1)) shouldBe true
            listOf<Any>(1).containsNone(listOf()) shouldBe true
            listOf<Any>(1).containsNone(listOf(2, 3)) shouldBe true

            listOf<Any>(1).containsNone(listOf(1)) shouldBe false
            listOf<Any>(1).containsNone(listOf(1, 2)) shouldBe false
            listOf<Any>(1).containsNone(listOf(2, 1)) shouldBe false
            listOf<Any>(1, 2, 3).containsNone(listOf(3, 4, 5)) shouldBe false
        }
    }
})
