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

    "a shift of bytes between the metadata and the content does not verify" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", "content", mapOf("title" to "t"))

        // The same bytes in the same order, cut at another place: only the lengths in the hash tell them apart
        s.meta("a.txt").writeText(s.meta("a.txt").readText().replace("\"title\":\"t\"", "\"title\":\"tc\""))
        File(s.dir, "a.txt").writeText("ontent")

        s.repo.verify("a.txt") shouldBe DepotVerification.Damaged
    }

    "only an intact file proves anything: no keyed hash and an unknown key are damaged, an erased key is unverifiable" {
        val s = setup()
        s.repo.putFile(anna, "a.txt", "content")
        s.repo.putFile(anna, "stripped.txt", "content")
        File(s.dir, "foreign.txt").writeText("f")

        s.repo.verify("foreign.txt") shouldBe DepotVerification.Damaged
        s.repo.verify("nothing.txt") shouldBe null

        // The entry removed by someone without the key
        s.meta("stripped.txt").writeText(s.meta("stripped.txt").readText().replace(Regex(",?\"depot-mac\":\"[^\"]+\""), ""))
        s.repo.verify("stripped.txt") shouldBe DepotVerification.Damaged

        // A key id made up by someone without the key: not "erased"
        val original = s.meta("a.txt").readText()
        s.meta("a.txt").writeText(original.replace(Regex("\"depot-key\":\"[^\"]+\""), "\"depot-key\":\"made-up\""))
        s.repo.verify("a.txt") shouldBe DepotVerification.Damaged
        s.meta("a.txt").writeText(original)

        s.keys.delete("customers/42") shouldBe true
        s.repo.verify("a.txt") shouldBe DepotVerification.Unverifiable
    }

    "a refused write makes no key" {
        val s = setup()

        shouldThrow<IllegalArgumentException> { s.repo.putFile(anna, "../out.txt", "content") }
        // 1947 bytes with subject and hash: fits with the keyed hash (90 more), not with the key's id as well
        shouldThrow<IllegalArgumentException> { s.repo.putFile(anna, "a.txt", "content", mapOf("note" to "x".repeat(1840))) }

        s.keys.size shouldBe 0
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
