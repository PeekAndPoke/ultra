package io.peekandpoke.ultra.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.common.SimpleLookup
import io.peekandpoke.ultra.depot.crypto.Aes256GcmV1
import io.peekandpoke.ultra.depot.crypto.DepotEncryption
import io.peekandpoke.ultra.depot.crypto.DepotEncryptionContext
import io.peekandpoke.ultra.depot.crypto.DepotSubjectKey
import io.peekandpoke.ultra.depot.crypto.EncryptingDepotRepository
import io.peekandpoke.ultra.depot.crypto.InMemoryDepotKeyStore
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification
import io.peekandpoke.ultra.depot.repos.DepotRawWrite
import io.peekandpoke.ultra.depot.repos.DepotRepository
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
import java.io.File
import java.nio.file.Files
import java.security.GeneralSecurityException

/** Encrypted files with a key per data subject, and shredding (ULTRA-00311). */
class EncryptingDepotRepositorySpec : StringSpec({

    class Customer

    fun customer(id: String) = DepotSubjectId.of(Customer::class, "customers", id)

    val anna = customer("42")
    val ben = customer("43")

    fun innerOver(dir: File, name: String = "files") = object : FileSystemRepository<Customer>(name, dir.absolutePath) {}

    class Setup(val dir: File, val keys: InMemoryDepotKeyStore, val repo: EncryptingDepotRepository<Customer>)

    fun setup(): Setup {
        val dir = tempdir()
        val keys = InMemoryDepotKeyStore()

        return Setup(dir, keys, object : EncryptingDepotRepository<Customer>(innerOver(dir), keys) {})
    }

    fun DepotContent.text() = String(bytesOrNull()!!)

    val secret = "the customer's very private contract"

    "a file is stored encrypted and reads back as it was written" {
        val s = setup()

        s.repo.putFile(anna, "docs/contract.txt", secret, mapOf("mime-type" to "text/plain"))

        String(File(s.dir, "docs/contract.txt").readBytes(), Charsets.ISO_8859_1) shouldNotContain "private"
        File(s.dir, "docs/contract.txt").length() shouldBe secret.length + 88L

        s.repo.getContent("docs/contract.txt").text() shouldBe secret
        (s.repo.getItem("docs/contract.txt") as DepotItem.File).getContent().text() shouldBe secret
        (s.repo.listItems("docs").single() as DepotItem.File).getContent().text() shouldBe secret
        s.repo.getFile("docs/contract.txt")!!.getContent().text() shouldBe secret
        s.repo.getFile("docs/contract.txt")!!.repo shouldBe s.repo
    }

    "the metadata names the subject and the method, and keeps the key's id to itself" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", secret, mapOf("title" to "t"))

        val meta = s.repo.getMeta("a.txt")!!

        meta.keys shouldBe setOf("title", "depot-subject", "depot-sha256", "depot-enc")
        meta["depot-subject"] shouldBe "customers/42"
        meta["depot-enc"] shouldBe "aes256gcm-v1"
        // The hash is of the stored bytes: checked without a key
        s.repo.verify("a.txt") shouldBe DepotVerification.Intact

        val raw = File(s.dir, ".meta/files/a.txt").readText()
        raw.contains("depot-key") shouldBe true
    }

    "every file has a key of its own, wrapped by its subject's key" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", secret)
        s.repo.putFile(anna, "b.txt", secret)
        val key = s.keys.getOrCreate("customers/42")

        fun fileKey(path: String) = Aes256GcmV1.fileKey(
            stored = File(s.dir, path).readBytes(),
            key = key,
            context = DepotEncryptionContext(method = "aes256gcm-v1", path = path, subject = "customers/42", keyId = key.id),
        ).toList()

        fileKey("a.txt").size shouldBe 32
        fileKey("a.txt") shouldNotBe fileKey("b.txt")
        fileKey("a.txt") shouldNotBe key.key.toList()
    }

    "a file carries its key material: old content under the metadata of a later write of the same subject still reads" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", "first", mapOf("title" to "one"))
        val first = File(s.dir, "a.txt").readBytes()
        s.repo.putFile(anna, "a.txt", "second", mapOf("title" to "two"))

        // As a crash between the two moves of the second write leaves it: the new metadata, the old content
        File(s.dir, "a.txt").writeBytes(first)

        s.repo.getContent("a.txt").text() shouldBe "first"
    }

    "a read that meets a write under way tries again; content that stays wrong is corrupted" {
        @OptIn(DepotRawWrite::class)
        class Flaky(val real: DepotRepository<Customer>, var wrong: Int) : DepotRepository<Customer> by real {
            override suspend fun getContent(path: String): DepotContent =
                if (wrong-- > 0) DepotContent.Found(ByteArray(200)) else real.getContent(path)
        }

        val dir = tempdir()
        val keys = InMemoryDepotKeyStore()
        object : EncryptingDepotRepository<Customer>(innerOver(dir), keys) {}.putFile(anna, "a.txt", secret)

        val twice = object : EncryptingDepotRepository<Customer>(Flaky(innerOver(dir), wrong = 2), keys) {}
        twice.getContent("a.txt").text() shouldBe secret

        val always = object : EncryptingDepotRepository<Customer>(Flaky(innerOver(dir), wrong = 3), keys) {}
        shouldThrow<DepotCorruptedException> { always.getContent("a.txt") }
    }

    "a repository reads what it wrote with a method of its own; one without that method refuses the file" {
        val custom = object : DepotEncryption by Aes256GcmV1 {
            override val name = "custom-v1"
        }
        val dir = tempdir()
        val keys = InMemoryDepotKeyStore()
        val repo = object : EncryptingDepotRepository<Customer>(innerOver(dir), keys, custom) {}

        repo.putFile(anna, "a.txt", secret)

        repo.getMeta("a.txt")!!["depot-enc"] shouldBe "custom-v1"
        repo.getContent("a.txt").text() shouldBe secret

        val plain = object : EncryptingDepotRepository<Customer>(innerOver(dir), keys) {}
        shouldThrowExactly<DepotException> { plain.getContent("a.txt") }
    }

    "shredding a subject makes its files unreadable for good, and leaves the others" {
        val s = setup()
        s.repo.putFile(anna, "anna.txt", secret)
        s.repo.putFile(ben, "ben.txt", "ben's")

        s.repo.shred(anna) shouldBe true
        s.repo.shred(anna) shouldBe false

        s.repo.getContent("anna.txt") shouldBe DepotContent.Shredded
        s.repo.getFile("anna.txt")!!.getContent() shouldBe DepotContent.Shredded
        s.repo.getContent("ben.txt").text() shouldBe "ben's"
        // The stored bytes are still what was written
        s.repo.verify("anna.txt") shouldBe DepotVerification.Intact

        // A new file of the same subject gets a new key; the old file stays unreadable
        s.repo.putFile(anna, "anna2.txt", "new")
        s.repo.getContent("anna2.txt").text() shouldBe "new"
        s.repo.getContent("anna.txt") shouldBe DepotContent.Shredded
    }

    "removing a file keeps the subject's key" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", secret)
        s.repo.putFile(anna, "b.txt", "b")

        s.repo.removeFile("a.txt") shouldBe true

        s.repo.getContent("a.txt") shouldBe DepotContent.NotFound
        s.repo.getContent("b.txt").text() shouldBe "b"
    }

    "system files and raw writes are refused" {
        val s = setup()

        shouldThrow<DepotException> { s.repo.putFile(DepotSubjectId.system, "sys.txt", "s") }
        @OptIn(DepotRawWrite::class)
        shouldThrow<DepotException> { s.repo.putRaw("raw.txt", "r".toByteArray(), mapOf("depot-subject" to "customers/42")) }

        File(s.dir, "sys.txt").exists() shouldBe false
        File(s.dir, "raw.txt").exists() shouldBe false
    }

    "a file that is not encrypted is refused, not handed out" {
        val dir = tempdir()
        val inner = innerOver(dir)
        val repo = object : EncryptingDepotRepository<Customer>(inner, InMemoryDepotKeyStore()) {}
        inner.putFile(anna, "plain.txt", "plain")
        File(dir, "foreign.txt").writeText("foreign")

        shouldThrowExactly<DepotException> { repo.getContent("plain.txt") }
        shouldThrowExactly<DepotException> { repo.getContent("foreign.txt") }
        repo.getContent("nothing.txt") shouldBe DepotContent.NotFound
    }

    "altered content does not decrypt" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", secret)
        val file = File(s.dir, "a.txt")

        file.writeBytes(file.readBytes().also { it[3] = (it[3] + 1).toByte() })

        shouldThrow<DepotCorruptedException> { s.repo.getContent("a.txt") }
    }

    "a file copied to another path, with its metadata, does not decrypt there" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", secret)

        File(s.dir, "a.txt").copyTo(File(s.dir, "copy.txt"))
        File(s.dir, ".meta/files/a.txt").copyTo(File(s.dir, ".meta/files/copy.txt"))

        s.repo.getContent("a.txt").text() shouldBe secret
        shouldThrow<DepotCorruptedException> { s.repo.getContent("copy.txt") }
    }

    "a file relabelled to another subject does not decrypt, with that subject's key id or with its own" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", secret)
        s.repo.putFile(ben, "b.txt", "ben's")
        val metaA = File(s.dir, ".meta/files/a.txt")
        val original = metaA.readText()

        // Anna's file said to be Ben's, the key's id left: the key is not Ben's
        metaA.writeText(original.replace("customers/42", "customers/43"))
        shouldThrow<DepotCorruptedException> { s.repo.getContent("a.txt") }

        // Anna's file with all of Ben's metadata: Ben's key does not open it
        metaA.writeText(File(s.dir, ".meta/files/b.txt").readText())
        shouldThrow<DepotCorruptedException> { s.repo.getContent("a.txt") }

        metaA.writeText(original)
        s.repo.getContent("a.txt").text() shouldBe secret
    }

    "an unknown method is refused" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", secret)
        val meta = File(s.dir, ".meta/files/a.txt")

        meta.writeText(meta.readText().replace("aes256gcm-v1", "rot13-v9"))

        shouldThrowExactly<DepotException> { s.repo.getContent("a.txt") }
    }

    "a corrupted file is a depot failure like the others" {
        (DepotCorruptedException() is DepotException) shouldBe true
    }

    "a link and the file it points to read the same content" {
        val s = setup()
        s.repo.putFile(anna, "target.txt", secret)
        Files.createSymbolicLink(File(s.dir, "link.txt").toPath(), File(s.dir, "target.txt").toPath())

        s.repo.getContent("link.txt").text() shouldBe secret

        s.repo.putFile(anna, "link.txt", "through the link")
        s.repo.getContent("target.txt").text() shouldBe "through the link"
    }

    "the method binds a file to its context, and two contexts never give the same bytes" {
        val key = DepotSubjectKey.create("customers/42")
        val context = DepotEncryptionContext(method = "aes256gcm-v1", path = "a.txt", subject = "customers/42", keyId = key.id)
        val encrypted = Aes256GcmV1.encrypt("plain".toByteArray(), key, context)

        String(Aes256GcmV1.decrypt(encrypted, key, context)) shouldBe "plain"

        listOf(
            DepotEncryptionContext("other", "a.txt", "customers/42", key.id),
            DepotEncryptionContext("aes256gcm-v1", "b.txt", "customers/42", key.id),
            DepotEncryptionContext("aes256gcm-v1", "a.txt", "customers/43", key.id),
            DepotEncryptionContext("aes256gcm-v1", "a.txt", "customers/42", "other-key"),
        ).forEach { other ->
            shouldThrow<GeneralSecurityException> { Aes256GcmV1.decrypt(encrypted, key, other) }
        }

        shouldThrow<GeneralSecurityException> {
            Aes256GcmV1.decrypt(encrypted, DepotSubjectKey.create("customers/42"), context)
        }
        shouldThrow<GeneralSecurityException> { Aes256GcmV1.decrypt(ByteArray(10), key, context) }
        shouldThrow<GeneralSecurityException> { Aes256GcmV1.decrypt(ByteArray(0), key, context) }

        DepotEncryptionContext("m", "ab", "c", "k").bytes().toList() shouldNotBe DepotEncryptionContext("m", "a", "bc", "k").bytes().toList()
    }

    "two repositories on one folder, or one inside the other, are refused by the depot" {
        // The lookup keeps one repository per class
        class RepoA(dir: File) : FileSystemRepository<Customer>("a", dir.absolutePath)

        class RepoB(dir: File) : FileSystemRepository<Customer>("b", dir.absolutePath)

        class Encrypting(dir: File) : EncryptingDepotRepository<Customer>(innerOver(dir, "enc"), InMemoryDepotKeyStore())

        val dir = tempdir()
        val nested = File(dir, "nested").also { it.mkdirs() }
        val other = tempdir()

        fun facade(vararg repos: DepotRepository<*>) = DepotFacade(SimpleLookup { repos.toList() })

        facade(RepoA(dir), RepoB(other)).getRepos().size shouldBe 2
        facade(Encrypting(dir), RepoB(other)).getRepos().size shouldBe 2

        shouldThrow<DepotException> { facade(RepoA(dir), RepoB(dir)) }
        shouldThrow<DepotException> { facade(RepoA(dir), RepoB(nested)) }
        shouldThrow<DepotException> { facade(RepoA(nested), RepoB(dir)) }

        // One name, one repository: a uri finds its repository by name
        class AlsoNamedA(dir: File) : FileSystemRepository<Customer>("a", dir.absolutePath)
        shouldThrow<DepotException> { facade(RepoA(dir), AlsoNamedA(other)) }

        // An encrypting repository and a plain one on its folder: the way around the encryption
        shouldThrow<DepotException> { facade(Encrypting(dir), RepoB(dir)) }
        shouldThrow<DepotException> { facade(RepoA(nested), Encrypting(dir)) }
    }
})
