package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.kType

class CursorSpec : StringSpec({

    "empty cursor has zero count and no items" {
        val cursor = Cursor.empty<String>()

        cursor.count shouldBe 0
        cursor.fullCount shouldBe 0
        cursor.toList().shouldBeEmpty()
    }

    "of(items) wraps items with correct count" {
        val items = listOf("a", "b", "c")
        val cursor = Cursor.of(items)

        cursor.count shouldBe 3
        cursor.toList() shouldBe items
    }

    "of(items) has null fullCount" {
        val cursor = Cursor.of(listOf(1, 2))

        cursor.fullCount shouldBe null
    }

    "of(type, items) wraps with explicit type" {
        val cursor = Cursor.of(type = kType<Int>(), items = listOf(1, 2, 3))

        cursor.count shouldBe 3
        cursor.toList() shouldBe listOf(1, 2, 3)
    }
})
