package io.peekandpoke.ultra.vault.tools

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.domain.DatabaseGraphModel

class DatabaseGraphBuilderSpec : StringSpec({

    "getGraph on an empty database returns an empty repo list" {
        val builder = DatabaseGraphBuilder(Database.withNoRepos, NullLog)

        builder.getGraph().repos.shouldBeEmpty()
    }

    "getGraph lists each repo with name, connection, and stored class fqn" {
        val db = Database.of { listOf(PersonRepo()) }
        val builder = DatabaseGraphBuilder(db, NullLog)

        val graph = builder.getGraph()

        graph.repos.size shouldBe 1

        val repo = graph.repos.single()
        repo.id.name shouldBe "persons"
        repo.id.connection shouldBe "default"
        repo.connection shouldBe "default"
        repo.storedClasses.size shouldBe 1
        repo.storedClasses.single().fqn shouldBe Person::class.java.name
    }

    "getGraph finds a direct Ref in a stored data class and resolves the target repo" {
        val db = Database.of { listOf(PersonRepo(), OrderRepo()) }
        val builder = DatabaseGraphBuilder(db, NullLog)

        val graph = builder.getGraph()

        val orderRepo = graph.repos.single { it.id.name == "orders" }
        val references = orderRepo.storedClasses.single().references

        references.size shouldBe 1
        val ref = references.single()
        ref.type shouldBe DatabaseGraphModel.Reference.Type.Direct
        ref.fqn shouldBe Person::class.java.name
        ref.repo?.name shouldBe "persons"
    }

    "getGraph includes a Ref entry even when no repo stores the target type" {
        // OrderRepo references Person but no PersonRepo is registered.
        val db = Database.of { listOf(OrderRepo()) }
        val builder = DatabaseGraphBuilder(db, NullLog)

        val graph = builder.getGraph()

        val orderRepo = graph.repos.single()
        val ref = orderRepo.storedClasses.single().references.single()

        ref.fqn shouldBe Person::class.java.name
        ref.repo shouldBe null
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

internal data class Person(val name: String)

internal data class Order(val buyer: Ref<Person>, val total: Int)

private class PersonRepo : Repository<Person> {
    override val name: String = "persons"
    override val connection: String = "default"
    override val storedType: TypeRef<Person> = kType()

    override suspend fun findById(id: String?): Stored<Person>? = null
    override suspend fun <X : Person> insert(new: New<X>): Stored<X> = error("not implemented")
    override suspend fun <X : Person> save(stored: Stored<X>): Stored<X> = error("not implemented")
    override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    override suspend fun removeAll(): RemoveResult = RemoveResult.empty
}

private class OrderRepo : Repository<Order> {
    override val name: String = "orders"
    override val connection: String = "default"
    override val storedType: TypeRef<Order> = kType()

    override suspend fun findById(id: String?): Stored<Order>? = null
    override suspend fun <X : Order> insert(new: New<X>): Stored<X> = error("not implemented")
    override suspend fun <X : Order> save(stored: Stored<X>): Stored<X> = error("not implemented")
    override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    override suspend fun removeAll(): RemoveResult = RemoveResult.empty
}
