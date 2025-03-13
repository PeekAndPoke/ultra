package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class MutableListsSpec : StringSpec({

    listOf(
        row(listOf(), arrayOf()),
        row(listOf(1), arrayOf()),
        row(listOf(), arrayOf(1)),
        row(listOf(1), arrayOf(2)),
        row(listOf(1, 2), arrayOf(3, 4))
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
        row(listOf(), arrayOf()),
        row(listOf(1), arrayOf()),
        row(listOf(), arrayOf(1)),
        row(listOf(1), arrayOf(2)),
        row(listOf(1, 2), arrayOf(3, 4))
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
