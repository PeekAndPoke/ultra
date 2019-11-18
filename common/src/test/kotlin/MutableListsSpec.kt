package de.peekandpoke.ultra.common

import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class MutableListsSpec : StringSpec({

    listOf(
        row(listOf(), listOf()),
        row(listOf(1), listOf()),
        row(listOf(), listOf(1)),
        row(listOf(1), listOf(2)),
        row(listOf(1, 2), listOf(3, 4))
    ).forEach { (input, args) ->

        "MutableList.push: vararg [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.push(*args.toTypedArray())

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe input.plus(args)
            }
        }

        "MutableList.push: array [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.push(args.toTypedArray())

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe input.plus(args)
            }
        }

        "MutableList.push: collection [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.push(args)

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
        row(listOf(), listOf()),
        row(listOf(1), listOf()),
        row(listOf(), listOf(1)),
        row(listOf(1), listOf(2)),
        row(listOf(1, 2), listOf(3, 4))
    ).forEach { (input, args) ->

        "MutableList.unshift: vararg [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.unshift(*args.toTypedArray())

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe args.plus(input)
            }
        }

        "MutableList.unshift: array [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.unshift(args.toTypedArray())

            assertSoftly {
                result shouldBe mutable

                mutable shouldBe args.plus(input)
            }
        }

        "MutableList.unshift: collection [$input] push [$args]" {

            val mutable = input.toMutableList()
            val result = mutable.unshift(args)

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
