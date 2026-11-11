package io.peekandpoke.funktor.cluster.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Stored

/** A stored entity as the data subject of a depot file (ULTRA-00311). */
class DepotSubjectsSpec : StringSpec({

    class Customer

    class Employee

    "a stored entity's subject id is its database id, so equal keys of two collections stay apart" {
        Stored(Customer(), "customers/42").asDepotSubject().value shouldBe "customers/42"
        Stored(Employee(), "employees/42").asDepotSubject().value shouldBe "employees/42"
        Stored(Customer(), "customers/42").asRef.asDepotSubject().value shouldBe "customers/42"
    }

    "an entity that is not stored yet is no subject" {
        shouldThrow<IllegalArgumentException> { New(Customer()).asDepotSubject() }
    }

    "putFile takes the stored entity and writes its id as the subject" {
        val repo = object : FileSystemRepository<Customer>("test", tempdir().absolutePath) {}

        repo.putFile(Stored(Customer(), "customers/42"), "a.txt", "a", mapOf("title" to "t"))
        repo.putFile(Stored(Customer(), "customers/43"), "b.txt", "b".toByteArray())

        repo.getMeta("a.txt") shouldBe mapOf("title" to "t", "depot-subject" to "customers/42")
        repo.getMeta("b.txt") shouldBe mapOf("depot-subject" to "customers/43")
    }
})
