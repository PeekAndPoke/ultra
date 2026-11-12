package io.peekandpoke.funktor.cluster.depot.keys

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.depot.DepotException
import io.peekandpoke.ultra.depot.crypto.DepotKeyStore
import io.peekandpoke.ultra.depot.crypto.EncryptingDepotRepository
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.profiling.DefaultQueryProfiler
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** The depot's subject keys in a database (ULTRA-00311), run against each backend. */
abstract class DepotKeyStoreSpecBase : StringSpec() {

    abstract val kontainer: Kontainer

    private val store: DepotKeyStore get() = kontainer.get(DepotKeyStore::class)

    private val repo: VaultDepotKeyStore.Repo get() = kontainer.get(VaultDepotKeyStore.Repo::class)

    /** A key store on this backend whose queries are recorded by [profiler]. */
    abstract fun profiledStore(profiler: QueryProfiler): DepotKeyStore

    /** A repository that answers "nothing removed" whatever happens, as Karango does for a failed removal. */
    private class RemovesNothing(real: VaultDepotKeyStore.Repo) : VaultDepotKeyStore.Repo by real {
        override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    }

    private class Customer

    /** A subject no other run has used. */
    private fun subject() = "customers/${UUID.randomUUID()}"

    init {
        "the cluster's key store is the database one" {
            store.shouldBeInstanceOf<VaultDepotKeyStore>()
        }

        "a subject gets one key, the same on every call, found again by its id" {
            val subject = subject()

            val first = store.getOrCreate(subject)
            val second = store.getOrCreate(subject)

            first.key.size shouldBe 32
            first.subject shouldBe subject
            second.id shouldBe first.id
            second.key.toList() shouldBe first.key.toList()

            val found = store.get(first.id)!!
            found.subject shouldBe subject
            found.key.toList() shouldBe first.key.toList()
        }

        "two subjects have different keys; a subject with unusual characters works" {
            val first = store.getOrCreate(subject())
            val second = store.getOrCreate("kunden/Zoë Müller ${UUID.randomUUID()}/a b")

            second.id shouldNotBe first.id
            second.key.toList() shouldNotBe first.key.toList()
            store.get(second.id)!!.key.toList() shouldBe second.key.toList()
        }

        "an erased key is never found again, also after the subject got a new one" {
            val subject = subject()
            val old = store.getOrCreate(subject)

            store.delete(subject) shouldBe true
            store.delete(subject) shouldBe false
            store.get(old.id) shouldBe null

            val new = store.getOrCreate(subject)

            new.id shouldNotBe old.id
            new.key.toList() shouldNotBe old.key.toList()
            store.get(old.id) shouldBe null
            store.get(new.id)!!.key.toList() shouldBe new.key.toList()
        }

        "a key that could not be erased is an exception, never 'there was none'" {
            val subject = subject()
            val failing = VaultDepotKeyStore(RemovesNothing(repo), Kronos.systemUtc)
            val key = failing.getOrCreate(subject)

            shouldThrow<DepotException> { failing.delete(subject) }

            // Still there, and still erasable for real
            store.get(key.id)!!.key.toList() shouldBe key.key.toList()
            failing.delete(subject()) shouldBe false
            store.delete(subject) shouldBe true
        }

        "the key store's queries are recorded without their values" {
            val profiler = DefaultQueryProfiler(explainQueries = true)
            val profiled = profiledStore(profiler)
            val subject = subject()

            val key = profiled.getOrCreate(subject)
            profiled.get(key.id)
            profiled.delete(subject)

            profiler.entries.shouldNotBeEmpty()
            profiler.entries.forEach { entry ->
                withClue(entry.query) {
                    entry.valuesWithheld shouldBe true
                    entry.vars?.values?.filterNotNull() shouldBe (entry.vars?.let { emptyList<Any>() })
                    entry.queryExplained shouldBe null
                }
            }
        }

        "a key record prints without its key" {
            val record = DepotSubjectKeyRecord(keyId = "id", subject = "customers/42", key = "c2VjcmV0", createdAt = Kronos.systemUtc.instantNow())

            record.toString() shouldNotContain "c2VjcmV0"
        }

        "an unknown id finds nothing" {
            store.get("no-such-key") shouldBe null
            store.get("${"0".repeat(64)}.${UUID.randomUUID()}") shouldBe null
        }

        "many first calls for one subject at once agree on one key" {
            val subject = subject()

            val keys = coroutineScope { (1..20).map { async { store.getOrCreate(subject) } }.awaitAll() }

            keys.map { it.id }.toSet() shouldHaveSize 1
            keys.map { it.key.toList() }.toSet() shouldHaveSize 1
        }

        "an encrypting repository on this store: written, read, shredded" {
            val dir = tempdir()
            val inner = object : FileSystemRepository<Customer>("files", dir.absolutePath) {}
            val repo = object : EncryptingDepotRepository<Customer>(inner, store) {}
            val anna = DepotSubjectId.of(Customer::class, "customers", UUID.randomUUID().toString())

            repo.putFile(anna, "a.txt", "secret")

            String(repo.getContent("a.txt").bytesOrNull()!!) shouldBe "secret"

            repo.shred(anna) shouldBe true

            repo.getContent("a.txt") shouldBe DepotContent.Shredded
        }
    }
}
