package io.peekandpoke.ultra.vault

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class RepositorySpec : StringSpec({

    // stores / getAllStoredClasses ////////////////////////////////////////////////////////////////

    "getAllStoredClasses includes the base type and its polymorphic children, transitively" {
        AnimalRepo().getAllStoredClasses() shouldContainAll listOf(
            Animal::class,
            Animal.Cat::class,
            Animal.Dog.Poodle::class,
        )
    }

    "stores matches the exact stored type" {
        AnimalRepo().stores(Animal::class) shouldBe true
    }

    "stores matches a direct child" {
        AnimalRepo().stores(Animal.Cat::class) shouldBe true
    }

    "stores matches a grandchild — not only direct subtypes" {
        // Poodle's only direct supertype is Dog, not Animal. Matching must be transitive,
        // otherwise it contradicts getAllStoredClasses, which lists Poodle.
        AnimalRepo().stores(Animal.Dog.Poodle::class) shouldBe true
    }

    "stores matches a sealed intermediate in the hierarchy" {
        AnimalRepo().stores(Animal.Dog::class) shouldBe true
    }

    "stores rejects an unrelated type" {
        AnimalRepo().stores(String::class) shouldBe false
    }

    "stores and getAllStoredClasses agree on every stored class" {
        val repo = AnimalRepo()

        repo.getAllStoredClasses().forEach { cls ->
            withClue(cls) { repo.stores(cls) shouldBe true }
        }
    }

    // saveIfModified //////////////////////////////////////////////////////////////////////////////

    "saveIfModified writes when the value changed" {
        val repo = RecordingAnimalRepo()
        val stored = Stored(value = Animal.Cat("Mimi"), _id = "animals/1")

        val result = repo.saveIfModified(stored) { Animal.Cat("Momo") }

        result.value shouldBe Animal.Cat("Momo")
        repo.saved shouldBe listOf(Animal.Cat("Momo"))
    }

    "saveIfModified does NOT write when the value is unchanged" {
        val repo = RecordingAnimalRepo()
        val stored = Stored(value = Animal.Cat("Mimi"), _id = "animals/1")

        val result = repo.saveIfModified(stored) { it }

        result shouldBe stored
        repo.saved shouldBe emptyList()
    }

    "saveIfModified does NOT write when modify returns an equal but distinct instance" {
        val repo = RecordingAnimalRepo()
        val stored = Stored(value = Animal.Cat("Mimi"), _id = "animals/1")

        // equality, not identity — this is why the value type needs proper equals
        val result = repo.saveIfModified(stored) { Animal.Cat("Mimi") }

        result shouldBe stored
        repo.saved shouldBe emptyList()
    }

    "saveIfModified preserves the database metadata of the original" {
        val repo = RecordingAnimalRepo()
        val stored = Stored(value = Animal.Cat("Mimi"), _id = "animals/1", _key = "1", _rev = "rev-7")

        val result = repo.saveIfModified(stored) { Animal.Cat("Momo") }

        result._id shouldBe "animals/1"
        result._key shouldBe "1"
        result._rev shouldBe "rev-7"
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

sealed class Animal {
    sealed class Dog : Animal() {
        data class Poodle(val name: String) : Dog()
    }

    data class Cat(val name: String) : Animal()
}

private open class AnimalRepo : Repository<Animal> {
    override val name: String = "animals"
    override val connection: String = "default"
    override val storedType: TypeRef<Animal> = kType()

    override suspend fun findById(id: String?): Stored<Animal>? = null
    override suspend fun <X : Animal> insert(new: New<X>): Stored<X> = error("not implemented")
    override suspend fun <X : Animal> save(stored: Stored<X>): Stored<X> = error("not implemented")
    override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    override suspend fun removeAll(): RemoveResult = RemoveResult.empty
}

/** Records what actually reached [save], so a skipped write is observable. */
private class RecordingAnimalRepo : AnimalRepo() {
    val saved = mutableListOf<Animal>()

    override suspend fun <X : Animal> save(stored: Stored<X>): Stored<X> {
        saved.add(stored.value)
        return stored
    }
}
