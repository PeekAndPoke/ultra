package io.peekandpoke.monko

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.slumber.SlumbererException
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.NullEntityCache

class MonkoCodecSpec : FreeSpec() {

    data class SimpleEntity(
        val name: String,
        val age: Int,
    )

    data class NestedEntity(
        val title: String,
        val child: SimpleEntity,
    )

    data class EntityWithNullable(
        val label: String,
        val optional: String?,
    )

    data class EntityWithList(
        val tags: List<String>,
    )

    data class EntityWithMap(
        val metadata: Map<String, Any>,
    )

    private fun createCodec(): MonkoCodec = MonkoCodec(
        config = getMonkoDefaultSlumberConfig(),
        database = Database.withNoRepos,
        entityCache = NullEntityCache,
    )

    init {
        "MonkoCodec" - {

            "slumber a simple entity" {
                val codec = createCodec()
                val entity = SimpleEntity(name = "Alice", age = 30)

                val result = codec.slumber(entity)

                result.shouldNotBeNull()
                result.shouldBeInstanceOf<Map<*, *>>()

                @Suppress("UNCHECKED_CAST")
                val map = result as Map<String, Any?>
                map["name"] shouldBe "Alice"
                map["age"] shouldBe 30
            }

            "awake a simple entity from map" {
                val codec = createCodec()
                val data = mapOf(
                    "name" to "Bob",
                    "age" to 25,
                )

                val result = codec.awake<SimpleEntity>(data)

                result.shouldNotBeNull()
                result.name shouldBe "Bob"
                result.age shouldBe 25
            }

            "round-trip a simple entity" {
                val codec = createCodec()
                val entity = SimpleEntity(name = "Charlie", age = 42)

                val slumbered = codec.slumber(entity)
                val awoken = codec.awake<SimpleEntity>(slumbered)

                awoken.shouldNotBeNull()
                awoken shouldBe entity
            }

            "slumber a nested entity" {
                val codec = createCodec()
                val entity = NestedEntity(
                    title = "Parent",
                    child = SimpleEntity(name = "Child", age = 5),
                )

                val result = codec.slumber(entity)

                result.shouldNotBeNull()
                result.shouldBeInstanceOf<Map<*, *>>()

                @Suppress("UNCHECKED_CAST")
                val map = result as Map<String, Any?>
                map["title"] shouldBe "Parent"

                val child = map["child"]
                child.shouldNotBeNull()
                child.shouldBeInstanceOf<Map<*, *>>()
            }

            "round-trip a nested entity" {
                val codec = createCodec()
                val entity = NestedEntity(
                    title = "Parent",
                    child = SimpleEntity(name = "Child", age = 5),
                )

                val slumbered = codec.slumber(entity)
                val awoken = codec.awake<NestedEntity>(slumbered)

                awoken.shouldNotBeNull()
                awoken shouldBe entity
            }

            "round-trip entity with nullable field present" {
                val codec = createCodec()
                val entity = EntityWithNullable(label = "Test", optional = "has-value")

                val slumbered = codec.slumber(entity)
                val awoken = codec.awake<EntityWithNullable>(slumbered)

                awoken.shouldNotBeNull()
                awoken shouldBe entity
            }

            "round-trip entity with nullable field absent" {
                val codec = createCodec()
                val entity = EntityWithNullable(label = "Test", optional = null)

                val slumbered = codec.slumber(entity)
                val awoken = codec.awake<EntityWithNullable>(slumbered)

                awoken.shouldNotBeNull()
                awoken shouldBe entity
            }

            "round-trip entity with list" {
                val codec = createCodec()
                val entity = EntityWithList(tags = listOf("a", "b", "c"))

                val slumbered = codec.slumber(entity)
                val awoken = codec.awake<EntityWithList>(slumbered)

                awoken.shouldNotBeNull()
                awoken shouldBe entity
            }

            "round-trip entity with empty list" {
                val codec = createCodec()
                val entity = EntityWithList(tags = emptyList())

                val slumbered = codec.slumber(entity)
                val awoken = codec.awake<EntityWithList>(slumbered)

                awoken.shouldNotBeNull()
                awoken shouldBe entity
            }

            "awake throws for null data" {
                val codec = createCodec()

                shouldThrow<AwakerException> {
                    codec.awake<SimpleEntity>(null)
                }
            }

            "slumber throws for null data" {
                val codec = createCodec()

                shouldThrow<SlumbererException> {
                    codec.slumber(kType<SimpleEntity>().type, null)
                }
            }

            "entityCache should be NullEntityCache by default" {
                val codec = createCodec()

                codec.entityCache shouldBe NullEntityCache
            }
        }
    }
}
