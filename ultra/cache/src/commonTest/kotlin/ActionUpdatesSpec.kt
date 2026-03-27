package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ActionUpdatesSpec : StringSpec() {
    init {
        "empty actions yields empty groupings" {
            val updates = FastCache.ActionUpdates<String, String>(actions = emptyList())

            updates.byKey shouldHaveSize 0
            updates.lastByKey shouldHaveSize 0
        }

        "byKey groups actions by their key" {
            val actions = listOf(
                FastCache.PutAction<String, String>("a", "1"),
                FastCache.ReadAction<String, String>("b", "2"),
                FastCache.PutAction<String, String>("a", "3"),
            )

            val updates = FastCache.ActionUpdates(actions)

            updates.byKey shouldHaveSize 2
            updates.byKey shouldContainKey "a"
            updates.byKey shouldContainKey "b"
            updates.byKey["a"]!!.size shouldBe 2
            updates.byKey["b"]!!.size shouldBe 1
        }

        "lastByKey returns only the last action per key" {
            val actions = listOf(
                FastCache.PutAction<String, String>("x", "first"),
                FastCache.ReadAction<String, String>("x", "second"),
                FastCache.RemoveAction<String, String>("x"),
            )

            val updates = FastCache.ActionUpdates(actions)

            updates.lastByKey shouldHaveSize 1
            updates.lastByKey["x"].shouldBeInstanceOf<FastCache.RemoveAction<String, String>>()
        }

        "lastByKey with multiple keys returns correct last actions" {
            val actions = listOf(
                FastCache.PutAction<String, Int>("a", 1),
                FastCache.PutAction<String, Int>("b", 10),
                FastCache.PutAction<String, Int>("a", 2),
                FastCache.ReadAction<String, Int>("b", 10),
                FastCache.PutAction<String, Int>("a", 3),
            )

            val updates = FastCache.ActionUpdates(actions)

            updates.lastByKey shouldHaveSize 2

            val lastA = updates.lastByKey["a"]
            lastA.shouldBeInstanceOf<FastCache.PutAction<String, Int>>()
            lastA.value shouldBe 3

            val lastB = updates.lastByKey["b"]
            lastB.shouldBeInstanceOf<FastCache.ReadAction<String, Int>>()
            lastB.value shouldBe 10
        }

        "actions list is preserved as-is" {
            val actions = listOf(
                FastCache.PutAction<String, String>("k", "v1"),
                FastCache.ReadAction<String, String>("k", "v1"),
            )

            val updates = FastCache.ActionUpdates(actions)

            updates.actions shouldContainExactly actions
        }
    }
}
