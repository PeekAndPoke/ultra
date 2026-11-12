package io.peekandpoke.ultra.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.crypto.InMemoryDepotKeyStore
import io.peekandpoke.ultra.depot.crypto.SignedDepotRepository
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.DepotRawWrite
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
import java.io.File

/** Files stored as they are, with a keyed hash that finds tampering (ULTRA-00311). */
class SignedDepotRepositorySpec : StringSpec({

    class Customer

    val anna = DepotSubjectId.of(Customer::class, "customers", "42")
    val ben = DepotSubjectId.of(Customer::class, "customers", "43")

    class Setup(val dir: File, val keys: InMemoryDepotKeyStore, val repo: SignedDepotRepository<Customer>)

    fun setup(): Setup {
        val dir = tempdir()
        val keys = InMemoryDepotKeyStore()
        val inner = object : FileSystemRepository<Customer>("files", dir.absolutePath) {}

        return Setup(dir, keys, object : SignedDepotRepository<Customer>(inner, keys) {})
    }

    fun Setup.meta(path: String) = File(dir, ".meta/files/$path")

    "a file is stored as it is, with a keyed hash, and verifies" {
        val s = setup()

        s.repo.putFile(anna, "a.txt", "content", mapOf("title" to "t"))

        File(s.dir, "a.txt").readText() shouldBe "content"
        String(s.repo.getContent("a.txt").bytesOrNull()!!) shouldBe "content"
        s.repo.getMeta("a.txt")!!.keys shouldBe setOf("title", "depot-subject", "depot-sha256", "depot-mac")
        s.repo.getMeta("a.txt")!!["depot-mac"]!!.startsWith("hmac-sha256-v1:") shouldBe true
        s.repo.verify("a.txt") shouldBe DepotVerification.Intact

        (s.repo.listItems("").single() as DepotItem.File).repo shouldBe s.repo
        s.repo.getFile("a.txt")!!.repo shouldBe s.repo
    }

    "changed content does not verify, also when the plain hash was made to fit" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", "content")

        File(s.dir, "a.txt").writeText("forged")
        s.repo.verify("a.txt") shouldBe DepotVerification.Damaged

        // What a plain hash cannot find: the one who changed the file also wrote a fitting depot-sha256
        val old = Regex("\"depot-sha256\":\"[0-9a-f]+\"")
        s.meta("a.txt").writeText(s.meta("a.txt").readText().replace(old, "\"depot-sha256\":\"${DepotMetadata.sha256("forged".toByteArray())}\""))

        s.repo.verify("a.txt") shouldBe DepotVerification.Damaged
    }

    "changed metadata, another subject and another path do not verify" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", "content", mapOf("title" to "t"))
        s.repo.putFile(ben, "b.txt", "ben's")
        val original = s.meta("a.txt").readText()

        s.meta("a.txt").writeText(original.replace("\"title\":\"t\"", "\"title\":\"x\""))
        s.repo.verify("a.txt") shouldBe DepotVerification.Damaged

        s.meta("a.txt").writeText(original.replace("customers/42", "customers/43"))
        s.repo.verify("a.txt") shouldBe DepotVerification.Damaged

        s.meta("a.txt").writeText(original)
        s.repo.verify("a.txt") shouldBe DepotVerification.Intact

        File(s.dir, "a.txt").copyTo(File(s.dir, "copy.txt"))
        s.meta("a.txt").copyTo(s.meta("copy.txt"))
        s.repo.verify("copy.txt") shouldBe DepotVerification.Damaged

        // Ben's file under Anna's metadata
        File(s.dir, "b.txt").copyTo(File(s.dir, "a.txt"), overwrite = true)
        s.repo.verify("a.txt") shouldBe DepotVerification.Damaged
    }

    "a file without a keyed hash, an erased key and a missing file each have their own answer" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", "content")
        File(s.dir, "foreign.txt").writeText("f")

        s.repo.verify("foreign.txt") shouldBe DepotVerification.NoHash
        s.repo.verify("nothing.txt") shouldBe null

        s.keys.delete("customers/42") shouldBe true
        s.repo.verify("a.txt") shouldBe DepotVerification.Unverifiable
    }

    "system files are signed too; a raw write is refused" {
        val s = setup()

        s.repo.putFile(DepotSubjectId.system, "sys.txt", "s")
        s.repo.verify("sys.txt") shouldBe DepotVerification.Intact

        @OptIn(DepotRawWrite::class)
        shouldThrow<DepotException> { s.repo.putRaw("raw.txt", "r".toByteArray(), mapOf("depot-subject" to "system")) }
        File(s.dir, "raw.txt").exists() shouldBe false
    }
})
