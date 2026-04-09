package io.peekandpoke.ultra.vault.slumber

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.DefaultEntityCache
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.VaultException

class RefCodecSpec : StringSpec({

    // Test fixtures ///////////////////////////////////////////////////////////////////////////////

    val entityId = "test_things/abc123"
    val entityKey = "abc123"
    val storedEntity = Stored(value = "hello-world", _id = entityId, _key = entityKey, _rev = "rev1")

    fun createCodec(
        stored: Stored<String>? = storedEntity,
        onFindById: () -> Unit = {},
    ): Codec {
        val repo = TestStringRepo(stored, onFindById)
        val database = Database.of { listOf(repo) }
        val cache = DefaultEntityCache()

        val attributes = TypedAttributes.of {
            add(VaultSlumberModule.DatabaseKey, database)
            add(VaultSlumberModule.EntityCacheKey, cache)
        }

        return Codec(
            config = SlumberConfig.default.copy(
                modules = listOf(VaultSlumberModule),
                attributes = attributes,
            )
        )
    }

    // awake (Ref deserialization) //////////////////////////////////////////////////////////////////

    "awake with entity id returns a Ref" {
        val codec = createCodec()
        val result = codec.awake<Ref<String>>(entityId)

        result.shouldBeInstanceOf<Ref<*>>()
        result._id shouldBe entityId
    }

    "awake creates a lazy Ref — resolver not called during awake" {
        var findByIdCalled = false
        val codec = createCodec(onFindById = { findByIdCalled = true })

        codec.awake<Ref<String>>(entityId)

        findByIdCalled shouldBe false
    }

    "awake + resolve returns the stored entity value" {
        val codec = createCodec()
        val ref = codec.awake<Ref<String>>(entityId)!!

        val resolved = ref.resolve()

        resolved shouldBe "hello-world"
    }

    "awake + resolve throws VaultException when entity not found" {
        val codec = createCodec(stored = null)
        val ref = codec.awake<Ref<String>>(entityId)!!

        try {
            ref.resolve()
            error("Expected VaultException")
        } catch (e: VaultException) {
            e.message shouldBe "Referenced entity not found: $entityId"
        }
    }

    "awake + resolve uses EntityCache — second Ref with same id reuses cache" {
        var findByIdCount = 0
        val codec = createCodec(onFindById = { findByIdCount++ })

        val ref1 = codec.awake<Ref<String>>(entityId)!!
        ref1.resolve()

        val ref2 = codec.awake<Ref<String>>(entityId)!!
        ref2.resolve()

        // EntityCache deduplicates: findById called only once
        findByIdCount shouldBe 1
    }

    // slumber (Ref serialization) /////////////////////////////////////////////////////////////////

    "slumber Ref returns its _id" {
        val codec = createCodec()
        val ref = Ref.eager(value = "test", _id = entityId, _key = entityKey, _rev = "rev1")

        val result = codec.slumber(ref)

        result shouldBe entityId
    }

    "slumber Ref created via asRef returns its _id" {
        val codec = createCodec()
        val ref = storedEntity.asRef

        val result = codec.slumber(ref)

        result shouldBe entityId
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

private class TestStringRepo(
    private val storedResult: Stored<String>?,
    private val onFindById: () -> Unit = {},
) : Repository<String> {
    override val name: String = "test_things"
    override val connection: String = "default"
    override val storedType: TypeRef<String> = kType()

    override suspend fun findById(id: String?): Stored<String>? {
        onFindById()
        return storedResult
    }

    override suspend fun <X : String> insert(new: New<X>): Stored<X> =
        error("Not implemented")

    override suspend fun <X : String> save(stored: Stored<X>): Stored<X> =
        error("Not implemented")

    override suspend fun remove(idOrKey: String): RemoveResult =
        error("Not implemented")

    override suspend fun removeAll(): RemoveResult =
        error("Not implemented")
}
