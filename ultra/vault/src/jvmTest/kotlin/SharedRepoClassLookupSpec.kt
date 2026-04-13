package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class SharedRepoClassLookupSpec : StringSpec({

    "getOrPut by Type caches the first value and does not call provider again" {
        val lookup = SharedRepoClassLookup()
        var callCount = 0

        val first = lookup.getOrPut(String::class) {
            callCount++
            StubRepo::class
        }
        val second = lookup.getOrPut(String::class) {
            callCount++
            StubRepo::class
        }

        callCount shouldBe 1
        first shouldBe StubRepo::class
        second shouldBe StubRepo::class
    }

    "getOrPut by Type stores independently for different types" {
        val lookup = SharedRepoClassLookup()

        lookup.getOrPut(String::class) { StubRepo::class }
        lookup.getOrPut(Int::class) { StubRepo::class }

        // Both calls should succeed without conflict
        val fromString = lookup.getOrPut(String::class) { error("should not be called") }
        val fromInt = lookup.getOrPut(Int::class) { error("should not be called") }

        fromString shouldBe StubRepo::class
        fromInt shouldBe StubRepo::class
    }

    "getOrPut by name caches the first value" {
        val lookup = SharedRepoClassLookup()
        var callCount = 0

        val first = lookup.getOrPut("repo-name") {
            callCount++
            StubRepo::class
        }
        val second = lookup.getOrPut("repo-name") {
            callCount++
            StubRepo::class
        }

        callCount shouldBe 1
        first shouldBe StubRepo::class
        second shouldBe StubRepo::class
    }

    "getOrPut by name and by type use independent caches" {
        val lookup = SharedRepoClassLookup()
        var typeCallCount = 0
        var nameCallCount = 0

        lookup.getOrPut(String::class) {
            typeCallCount++
            StubRepo::class
        }
        lookup.getOrPut("String") {
            nameCallCount++
            StubRepo::class
        }

        typeCallCount shouldBe 1
        nameCallCount shouldBe 1
    }

    "getOrPut by Type caches a null provider result without NPE" {
        val lookup = SharedRepoClassLookup()
        var callCount = 0

        val first = lookup.getOrPut(String::class) {
            callCount++
            null
        }
        val second = lookup.getOrPut(String::class) {
            callCount++
            null
        }

        // The negative result is cached -- the provider is only called once.
        first shouldBe null
        second shouldBe null
        callCount shouldBe 1
    }

    "getOrPut by name caches a null provider result without NPE" {
        val lookup = SharedRepoClassLookup()
        var callCount = 0

        val first = lookup.getOrPut("missing") {
            callCount++
            null
        }
        val second = lookup.getOrPut("missing") {
            callCount++
            null
        }

        first shouldBe null
        second shouldBe null
        callCount shouldBe 1
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

private class StubRepo : Repository<String> {
    override val name: String = "stub"
    override val connection: String = "default"
    override val storedType: TypeRef<String> = kType()

    override suspend fun findById(id: String?): Stored<String>? = null
    override suspend fun <X : String> insert(new: New<X>): Stored<X> = error("not implemented")
    override suspend fun <X : String> save(stored: Stored<X>): Stored<X> = error("not implemented")
    override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    override suspend fun removeAll(): RemoveResult = RemoveResult.empty
}
