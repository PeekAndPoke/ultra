package io.peekandpoke.ultra.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification
import io.peekandpoke.ultra.depot.repos.DepotInvalidMetadataException
import io.peekandpoke.ultra.depot.repos.DepotInvalidPathException
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.DepotRepository
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CyclicBarrier
import kotlinx.coroutines.runBlocking

/** A file's metadata: the rules, and the file system repository's `.meta` tree (ULTRA-00311). */
class DepotMetadataSpec : StringSpec({

    class Customer

    fun repoOver(dir: File) = object : FileSystemRepository<Customer>("test", dir.absolutePath) {}

    fun customer(id: String) = DepotSubjectId.of(Customer::class, "customers", id)

    val anna = customer("42")

    "keys are 1 to 64 characters of a-z, 0-9 and '-'; depot- keys are reserved" {
        listOf("", "Upper", "under_score", "dot.ted", "späce", "a".repeat(65)).forEach { key ->
            shouldThrow<DepotInvalidMetadataException> { DepotMetadata.validate(mapOf(key to "x")) }
        }
        listOf("depot-subject", "depot-key", "depot-anything").forEach { key ->
            shouldThrow<DepotInvalidMetadataException> { DepotMetadata.validate(mapOf(key to "x")) }
        }

        val ok = mapOf("mime-type" to "application/pdf", "a".repeat(64) to "x", "0" to "")
        DepotMetadata.validate(ok) shouldBe ok
    }

    "everything stored stays within S3's 2 KB, measured percent-encoded" {
        // 2048 bytes exactly: a 1-character key and 2047 ASCII characters
        DepotMetadata.validateSize(mapOf("k" to "v".repeat(2047)))
        shouldThrow<DepotInvalidMetadataException> { DepotMetadata.validateSize(mapOf("k" to "v".repeat(2048))) }
        // "ä" is 6 bytes percent-encoded (%C3%A4): 342 of them pass 2048
        shouldThrow<DepotInvalidMetadataException> { DepotMetadata.validateSize(mapOf("k" to "ä".repeat(342))) }
    }

    "metadata round-trips, with the subject's id added" {
        val repo = repoOver(tempdir())

        repo.putFile(anna, "docs/contract.pdf", "pdf".toByteArray(), mapOf("mime-type" to "application/pdf", "title" to "Vertrag März"))

        repo.getMeta("docs/contract.pdf")?.minus("depot-sha256") shouldBe mapOf(
            "mime-type" to "application/pdf",
            "title" to "Vertrag März",
            "depot-subject" to "customers/42",
        )
    }

    "the metadata tree mirrors the files' paths" {
        val dir = tempdir()
        val repo = repoOver(dir)

        repo.putFile(anna, "docs/2026/contract.pdf", "pdf")
        repo.putFile(anna, "contract.pdf", "other", mapOf("title" to "top"))

        File(dir, ".meta/files/docs/2026/contract.pdf").isFile shouldBe true
        repo.getMeta("docs/2026/contract.pdf")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "customers/42")
    }

    "a write without metadata still records the subject" {
        val repo = repoOver(tempdir())

        repo.putFile(anna, "a.txt", "a")

        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "customers/42")
    }

    "a file the depot did not write has no subject recorded; no file reads as null" {
        val dir = tempdir()
        File(dir, "old.txt").writeText("from before")
        val repo = repoOver(dir)

        repo.getMeta("old.txt") shouldBe emptyMap()
        repo.getMeta("nothing.txt") shouldBe null
    }

    "a refused key writes nothing: neither the file nor its metadata" {
        val dir = tempdir()
        val repo = repoOver(dir)

        shouldThrow<DepotInvalidMetadataException> { repo.putFile(anna, "a.txt", "a", mapOf("depot-subject" to "someone-else")) }
        shouldThrow<DepotInvalidMetadataException> { repo.putFile(anna, "a.txt", "a", mapOf("Bad Key" to "x")) }

        File(dir, "a.txt").exists() shouldBe false
        File(dir, ".meta").exists() shouldBe false
    }

    "the metadata tree is not listed and cannot be addressed" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "a.txt", "a")

        File(dir, ".meta/files/a.txt").isFile shouldBe true
        repo.listItems("").map { it.name } shouldContainExactly listOf("a.txt")

        // Under any spelling a case-insensitive file system or Windows would accept
        listOf(".meta", ".META", ".Meta", ".meta.", ".meta ", "/.meta", "\\.meta").forEach { meta ->
            shouldThrow<DepotInvalidPathException> { repo.getContent("$meta/files/a.txt") }
            shouldThrow<DepotInvalidPathException> { repo.listItems(meta) }
            shouldThrow<DepotInvalidPathException> { repo.putFile(anna, "$meta/files/a.txt", "{}") }
            shouldThrow<DepotInvalidPathException> { repo.removeFile("$meta/files/a.txt") }
        }

        // Only the first segment is reserved
        repo.putFile(anna, "docs/.meta/x.txt", "x")
        repo.getMeta("docs/.meta/x.txt")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "customers/42")
        repo.listItems("docs").map { it.name } shouldBe listOf(".meta")
    }

    "removing a file removes its metadata" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "a.txt", "a", mapOf("title" to "first"))

        repo.removeFile("a.txt") shouldBe true

        File(dir, ".meta/files/a.txt").exists() shouldBe false
        repo.getMeta("a.txt") shouldBe null
    }

    "a second write replaces the metadata" {
        val repo = repoOver(tempdir())
        repo.putFile(anna, "a.txt", "a", mapOf("title" to "first", "note" to "n"))

        repo.putFile(customer("43"), "a.txt", "b", mapOf("title" to "second"))

        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "second", "depot-subject" to "customers/43")
    }

    "the system subject is accepted by a typed repository, and by one seen without its type" {
        val repo = repoOver(tempdir())
        val untyped: DepotRepository<*> = repo

        repo.putFile(DepotSubjectId.system, "sys.txt", "s")
        untyped.putFile(DepotSubjectId.system, "sys2.txt", "s")

        repo.getMeta("sys.txt")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "system")
        repo.getMeta("sys2.txt")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "system")
    }

    "a subject id is namespace/id; blank parts, a slash in the namespace and Nothing as the kind are refused" {
        DepotSubjectId.of(Customer::class, "customers", "42").value shouldBe "customers/42"
        DepotSubjectId.of(Customer::class, "customers", "a/b").value shouldBe "customers/a/b"

        shouldThrow<IllegalArgumentException> { DepotSubjectId.of(Customer::class, "", "42") }
        shouldThrow<IllegalArgumentException> { DepotSubjectId.of(Customer::class, " ", "42") }
        shouldThrow<IllegalArgumentException> { DepotSubjectId.of(Customer::class, "a/b", "42") }
        shouldThrow<IllegalArgumentException> { DepotSubjectId.of(Customer::class, "customers", "") }
        shouldThrow<IllegalArgumentException> { DepotSubjectId.of(Customer::class, "customers", " ") }
        shouldThrow<IllegalArgumentException> { DepotSubjectId.of(Nothing::class, "customers", "42") }
        // An unpaired surrogate would be stored as '?'
        shouldThrow<IllegalArgumentException> { DepotSubjectId.of(Customer::class, "customers", "a" + Char(0xD800)) }
    }

    "a system file records its subject like any other; a system write over a customer's file replaces the subject" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "a.txt", "a", mapOf("title" to "first"))

        repo.putFile(DepotSubjectId.system, "a.txt", "b")

        File(dir, ".meta/files/a.txt").isFile shouldBe true
        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "system")
    }

    "a write stores the content's hash; verify tells an intact file from a damaged one and one without a hash" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "a.txt", "a")

        repo.getMeta("a.txt") shouldBe mapOf(
            "depot-subject" to "customers/42",
            "depot-sha256" to "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
        )
        repo.verify("a.txt") shouldBe DepotVerification.Intact

        File(dir, "a.txt").writeText("b")
        repo.verify("a.txt") shouldBe DepotVerification.Damaged

        File(dir, "foreign.txt").writeText("f")
        repo.verify("foreign.txt") shouldBe DepotVerification.NoHash

        repo.verify("nothing.txt") shouldBe null
    }

    "the subject's id counts towards the size limit" {
        val repo = repoOver(tempdir())

        // 13 for the key "depot-subject" + 1954 ("customers%2F" is 12, with the slash percent-encoded), and 12 + 64
        // for the hash = 2043 bytes; "k" + 5 more = 2049
        shouldThrow<DepotInvalidMetadataException> {
            repo.putFile(customer("s".repeat(1942)), "a.txt", "a", mapOf("k" to "v".repeat(5)))
        }
        repo.putFile(customer("s".repeat(1942)), "a.txt", "a", mapOf("k" to "v".repeat(4)))
    }

    "a mime type must look like type/subtype" {
        listOf("text", "text/html\r\nX-Evil: 1", "a b/c", "").forEach { mime ->
            shouldThrow<DepotInvalidMetadataException> { DepotMetadata.validate(mapOf("mime-type" to mime)) }
        }
        DepotMetadata.validate(mapOf("mime-type" to "image/svg+xml"))
    }

    "a link that aliases the root does not reach or show the metadata tree" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "a.txt", "a")
        Files.createSymbolicLink(File(dir, "self").toPath(), dir.toPath())

        repo.listItems("self").map { it.name }.sorted() shouldContainExactly listOf("a.txt", "self")
        shouldThrow<DepotInvalidPathException> { repo.getContent("self/.meta/files/a.txt") }
        shouldThrow<DepotInvalidPathException> { repo.putFile(anna, "self/.meta/files/a.txt", "{}") }
    }

    "a metadata tree that is a link out of the root is refused" {
        val dir = tempdir()
        val outside = tempdir()
        Files.createSymbolicLink(File(dir, ".meta").toPath(), outside.toPath())

        shouldThrow<DepotInvalidPathException> { repoOver(dir).putFile(anna, "a.txt", "a") }

        outside.list()!!.toList() shouldBe emptyList()
        File(dir, "a.txt").exists() shouldBe false

        // Nor is metadata read through it
        File(dir, "b.txt").writeText("b")
        File(outside, "files").mkdirs()
        File(outside, "files/b.txt").writeText("""{"depot-subject":"forged"}""")

        shouldThrow<DepotInvalidPathException> { repoOver(dir).getMeta("b.txt") }
    }

    "a link and the file it points to share one metadata; removing the link keeps it" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "target.txt", "t", mapOf("title" to "one"))
        Files.createSymbolicLink(File(dir, "link.txt").toPath(), File(dir, "target.txt").toPath())

        repo.getMeta("link.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "one", "depot-subject" to "customers/42")

        repo.putFile(customer("43"), "link.txt", "t2")
        repo.getMeta("target.txt")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "customers/43")

        repo.removeFile("link.txt") shouldBe true
        repo.getMeta("target.txt")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "customers/43")
    }

    "a file and a folder whose names differ by a suffix do not collide in the metadata tree" {
        val repo = repoOver(tempdir())

        repo.putFile(anna, "report", "r")
        repo.putFile(anna, "report.json/part1", "p")
        repo.putFile(anna, "report.tmp/part1", "p")

        repo.getMeta("report")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "customers/42")
        repo.getMeta("report.json/part1")?.minus("depot-sha256") shouldBe mapOf("depot-subject" to "customers/42")
    }

    "a write to a folder or the root is refused and leaves nothing behind" {
        val dir = tempdir()
        val repo = repoOver(dir)
        File(dir, "docs").mkdirs()

        shouldThrow<DepotInvalidPathException> { repo.putFile(anna, "docs", "x") }
        shouldThrow<DepotInvalidPathException> { repo.putFile(anna, "", "x") }

        File(dir, ".meta").exists() shouldBe false
    }

    "metadata left without its file reads as no file, and a remove clears it" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "docs/a.txt", "a")
        File(dir, "docs/a.txt").delete()

        repo.getMeta("docs/a.txt") shouldBe null
        repo.removeFile("docs/a.txt") shouldBe false

        File(dir, ".meta/files/docs").exists() shouldBe false
    }

    "a write that fails changes neither the content nor the metadata" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "a.txt", "old", mapOf("title" to "old"))

        shouldThrow<DepotInvalidMetadataException> {
            repo.putFile(customer("43"), "a.txt", "new", mapOf("title" to "x".repeat(3000)))
        }

        File(dir, "a.txt").readText() shouldBe "old"
        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "old", "depot-subject" to "customers/42")
    }

    "a write whose content cannot be moved in place puts the old metadata back, and leaves no temporary file" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "locked/a.txt", "old", mapOf("title" to "old"))
        val locked = File(dir, "locked")

        locked.setWritable(false)

        try {
            // Not for a user the file system lets write anyway (root)
            if (!Files.isWritable(locked.toPath())) {
                shouldThrow<Exception> { repo.putFile(customer("43"), "locked/a.txt", "new", mapOf("title" to "new")) }
                shouldThrow<Exception> { repo.putFile(customer("43"), "locked/b.txt", "new") }

                File(locked, "a.txt").readText() shouldBe "old"
                repo.getMeta("locked/a.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "old", "depot-subject" to "customers/42")
                File(dir, ".meta/files/locked/b.txt").exists() shouldBe false
                File(dir, ".meta/tmp").list()!!.toList() shouldBe emptyList()
            }
        } finally {
            locked.setWritable(true)
        }
    }

    "a file is created like any file of the process, not owner-only" {
        val dir = tempdir()
        repoOver(dir).putFile(anna, "a.txt", "a")
        val plain = File(dir, "plain.txt").also { it.writeText("p") }

        Files.getPosixFilePermissions(File(dir, "a.txt").toPath()) shouldBe Files.getPosixFilePermissions(plain.toPath())
    }

    "temporary files a crashed write left behind are swept; one of a write under way stays" {
        val dir = tempdir()
        val tmp = File(dir, ".meta/tmp").also { it.mkdirs() }
        val stale = File(tmp, "put-stale.tmp").also { it.writeText("left") }.also { it.setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1000L) }
        val fresh = File(tmp, "put-fresh.tmp").also { it.writeText("under way") }

        repoOver(dir).putFile(anna, "a.txt", "a")

        stale.exists() shouldBe false
        fresh.exists() shouldBe true
    }

    "a temporary folder that is a link out of the root is refused, and nothing is written there" {
        val dir = tempdir()
        val outside = tempdir()
        File(dir, ".meta").mkdirs()
        Files.createSymbolicLink(File(dir, ".meta/tmp").toPath(), outside.toPath())

        shouldThrow<DepotInvalidPathException> { repoOver(dir).putFile(anna, "a.txt", "a") }

        outside.list()!!.toList() shouldBe emptyList()
        File(dir, "a.txt").exists() shouldBe false
    }

    "what getMeta returns goes back into a write without the depot's own entries" {
        val repo = repoOver(tempdir())
        repo.putFile(anna, "a.txt", "a", mapOf("title" to "t"))

        repo.putFile(anna, "a.txt", "b", DepotMetadata.withoutReserved(repo.getMeta("a.txt")!!))

        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "t", "depot-subject" to "customers/42")
    }

    "removing a path below a file keeps that file's metadata; so does a refused write below it" {
        val repo = repoOver(tempdir())
        repo.putFile(anna, "a.txt", "a", mapOf("title" to "t"))

        repo.removeFile("a.txt/x") shouldBe false
        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "t", "depot-subject" to "customers/42")

        shouldThrow<Exception> { repo.putFile(anna, "a.txt/x", "x") }
        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "t", "depot-subject" to "customers/42")
    }

    "two repository objects over one folder: writers of one file still take turns" {
        val dir = tempdir()
        val repos = listOf(repoOver(dir), repoOver(dir))

        repeat(400) { round ->
            val start = CyclicBarrier(2)
            val threads = (1..2).map { n ->
                Thread {
                    start.await()
                    runBlocking { repos[n - 1].putFile(customer("$n"), "r$round.txt", "content-$n", mapOf("n" to "$n")) }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }

            val n = File(dir, "r$round.txt").readText().removePrefix("content-")
            repos[0].getMeta("r$round.txt")?.minus("depot-sha256") shouldBe mapOf("n" to n, "depot-subject" to "customers/$n")
            repos[0].verify("r$round.txt") shouldBe DepotVerification.Intact
        }
    }

    "two writers of one file at once: the file ends with one writer's content and that writer's metadata" {
        val dir = tempdir()
        val repo = repoOver(dir)

        repeat(400) { round ->
            val start = CyclicBarrier(2)
            val threads = (1..2).map { n ->
                Thread {
                    start.await()
                    runBlocking { repo.putFile(customer("$n"), "r$round.txt", "content-$n", mapOf("n" to "$n")) }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }

            val n = File(dir, "r$round.txt").readText().removePrefix("content-")
            repo.getMeta("r$round.txt")?.minus("depot-sha256") shouldBe mapOf("n" to n, "depot-subject" to "customers/$n")
            repo.verify("r$round.txt") shouldBe DepotVerification.Intact
        }

        File(dir, ".meta/tmp").list()!!.toList() shouldBe emptyList()
    }
})
