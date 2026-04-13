package io.peekandpoke.ultra.vault

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class DatabaseSpec : StringSpec() {

    private val k
        get() = kontainer {
            singleton(Kronos::class) { Kronos.systemUtc }

            ultraLogging()
            ultraVault(VaultConfig())
        }.create()

    init {
        "Simple creation of a Database" {
            k.get(Database::class).shouldBeInstanceOf<Database>()
        }

        // Database.withNoRepos ////////////////////////////////////////////////////////////////////

        "Database.withNoRepos has no repositories" {
            val db = Database.withNoRepos

            db.getRepositories().shouldBeEmpty()
        }

        // Database.of /////////////////////////////////////////////////////////////////////////////

        "Database.of creates a Database with given repos" {
            val repo = TestStringRepo()
            val db = Database.of { listOf(repo) }

            db.getRepositories() shouldHaveSize 1
            db.getRepositories().shouldContain(repo)
        }

        // getRepositories /////////////////////////////////////////////////////////////////////////

        "getRepositories returns all registered repositories" {
            val repo1 = TestStringRepo()
            val repo2 = TestIntRepo()
            val db = Database.of { listOf(repo1, repo2) }

            val repos = db.getRepositories()
            repos shouldHaveSize 2
        }

        // getRepository by name ///////////////////////////////////////////////////////////////////

        "getRepository by name returns matching repo" {
            val repo = TestStringRepo()
            val db = Database.of { listOf(repo) }

            val found = db.getRepository("test_strings")
            found shouldBe repo
        }

        "getRepository by name throws VaultException when not found" {
            val db = Database.withNoRepos

            shouldThrow<VaultException> {
                db.getRepository("missing")
            }
        }

        "getRepository by name caches negative lookups without NPE" {
            val db = Database.of { listOf(TestStringRepo()) }

            // Two consecutive lookups for a missing name must both throw cleanly.
            shouldThrow<VaultException> { db.getRepository("missing") }
            shouldThrow<VaultException> { db.getRepository("missing") }
        }

        // getRepository by class //////////////////////////////////////////////////////////////////

        "getRepository by class returns matching repo" {
            val repo = TestStringRepo()
            val db = Database.of { listOf(repo) }

            val found = db.getRepository(TestStringRepo::class)
            found shouldBe repo
        }

        "getRepository by class throws when not found" {
            val db = Database.withNoRepos

            shouldThrow<VaultException> {
                db.getRepository(TestStringRepo::class)
            }
        }

        // hasRepositoryStoring ////////////////////////////////////////////////////////////////////

        "hasRepositoryStoring returns true for stored type" {
            val repo = TestStringRepo()
            val db = Database.of { listOf(repo) }

            db.hasRepositoryStoring(String::class) shouldBe true
        }

        "hasRepositoryStoring returns false for unknown type" {
            val db = Database.of { listOf(TestStringRepo()) }

            db.hasRepositoryStoring(Int::class) shouldBe false
        }

        "hasRepositoryStoring caches negative lookups without NPE" {
            val db = Database.of { listOf(TestStringRepo()) }

            // Two consecutive lookups for a missing type must return false without throwing.
            db.hasRepositoryStoring(Int::class) shouldBe false
            db.hasRepositoryStoring(Int::class) shouldBe false
        }

        // getRepositoryStoring / getRepositoryStoringOrNull ///////////////////////////////////////

        "getRepositoryStoring returns the repo that stores the given type" {
            val repo = TestStringRepo()
            val db = Database.of { listOf(repo) }

            db.getRepositoryStoring(String::class) shouldBe repo
        }

        "getRepositoryStoring throws VaultException when no repo stores the type" {
            val db = Database.of { listOf(TestStringRepo()) }

            shouldThrow<VaultException> {
                db.getRepositoryStoring(Int::class)
            }
        }

        "getRepositoryStoringOrNull returns null when no repo stores the type" {
            val db = Database.of { listOf(TestStringRepo()) }

            db.getRepositoryStoringOrNull(Int::class) shouldBe null
        }

        // ensureId ////////////////////////////////////////////////////////////////////////////////

        "Repository.ensureId returns idOrKey unchanged if it contains a slash" {
            val repo = TestStringRepo()

            repo.ensureId("collection/key123") shouldBe "collection/key123"
        }

        "Repository.ensureId prepends repo name if idOrKey has no slash" {
            val repo = TestStringRepo()

            repo.ensureId("key123") shouldBe "test_strings/key123"
        }
    }
}

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

private class TestStringRepo : Repository<String> {
    override val name: String = "test_strings"
    override val connection: String = "default"
    override val storedType: TypeRef<String> = kType()

    override suspend fun findById(id: String?): Stored<String>? = null
    override suspend fun <X : String> insert(new: New<X>): Stored<X> = error("not implemented")
    override suspend fun <X : String> save(stored: Stored<X>): Stored<X> = error("not implemented")
    override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    override suspend fun removeAll(): RemoveResult = RemoveResult.empty
}

private class TestIntRepo : Repository<Int> {
    override val name: String = "test_ints"
    override val connection: String = "default"
    override val storedType: TypeRef<Int> = kType()

    override suspend fun findById(id: String?): Stored<Int>? = null
    override suspend fun <X : Int> insert(new: New<X>): Stored<X> = error("not implemented")
    override suspend fun <X : Int> save(stored: Stored<X>): Stored<X> = error("not implemented")
    override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    override suspend fun removeAll(): RemoveResult = RemoveResult.empty
}
