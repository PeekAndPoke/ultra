package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MutableListsSpec : StringSpec({

    listOf(
        tuple(listOf(), arrayOf()),
        tuple(listOf(1), arrayOf()),
        tuple(listOf(), arrayOf(1)),
        tuple(listOf(1), arrayOf(2)),
        tuple(listOf(1, 2), arrayOf(3, 4))
    ).forEach { (input, args) ->

        "MutableList.push: vararg [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.push(*args)

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe input.plus(args)
            }
        }

        "MutableList.push: array [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.push(args.toList())

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe input.plus(args)
            }
        }

        "MutableList.push: collection [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.push(args.toList())

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe input.plus(args)
            }
        }
    }

    "MutableList.pop" {

        val subject = mutableListOf(1, 2)

        assertSoftly {
            subject.pop() shouldBe 2
            subject.pop() shouldBe 1
            subject.pop() shouldBe null
        }
    }

    listOf(
        tuple(listOf(), arrayOf()),
        tuple(listOf(1), arrayOf()),
        tuple(listOf(), arrayOf(1)),
        tuple(listOf(1), arrayOf(2)),
        tuple(listOf(1, 2), arrayOf(3, 4))
    ).forEach { (input, args) ->

        "MutableList.unshift: vararg [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.unshift(*args)

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe args.plus(input)
            }
        }

        "MutableList.unshift: array [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.unshift(args)

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe args.plus(input)
            }
        }

        "MutableList.unshift: collection [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.unshift(args.toList())

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe args.plus(input)
            }
        }
    }

    "MutableList.shift" {

        val subject = mutableListOf(1, 2)

        assertSoftly {
            subject.shift() shouldBe 1
            subject.shift() shouldBe 2
            subject.shift() shouldBe null
        }
    }
})
