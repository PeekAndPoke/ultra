package io.peekandpoke.ultra.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.DepotInvalidMetadataException
import io.peekandpoke.ultra.depot.repos.DepotInvalidPathException
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
import java.io.File

/** A file's metadata: the rules, and the file system repository's `.meta` tree (ULTRA-00311). */
class DepotMetadataSpec : StringSpec({

    class Customer

    fun repoOver(dir: File) = object : FileSystemRepository<Customer>("test", dir.absolutePath) {}

    val anna = DepotSubjectId<Customer>("customer-42")

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

        repo.getMeta("docs/contract.pdf") shouldBe mapOf(
            "mime-type" to "application/pdf",
            "title" to "Vertrag März",
            "depot-subject" to "customer-42",
        )
    }

    "a write without metadata still records the subject" {
        val repo = repoOver(tempdir())

        repo.putFile(anna, "a.txt", "a")

        repo.getMeta("a.txt") shouldBe mapOf("depot-subject" to "customer-42")
    }

    "a file stored without metadata reads as an empty map; no file reads as null" {
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

        File(dir, ".meta/a.txt.json").isFile shouldBe true
        repo.listItems("").map { it.name } shouldContainExactly listOf("a.txt")

        shouldThrow<DepotInvalidPathException> { repo.getContent(".meta/a.txt.json") }
        shouldThrow<DepotInvalidPathException> { repo.listItems(".meta") }
        shouldThrow<DepotInvalidPathException> { repo.putFile(anna, "/.meta/a.txt.json", "{}") }
        shouldThrow<DepotInvalidPathException> { repo.removeFile(".meta/a.txt.json") }

        // Only the first segment is reserved
        repo.putFile(anna, "docs/.meta/x.txt", "x")
        repo.getMeta("docs/.meta/x.txt") shouldBe mapOf("depot-subject" to "customer-42")
    }

    "removing a file removes its metadata; a new file of that name starts fresh" {
        val dir = tempdir()
        val repo = repoOver(dir)
        repo.putFile(anna, "a.txt", "a", mapOf("title" to "first"))

        repo.removeFile("a.txt") shouldBe true

        File(dir, ".meta/a.txt.json").exists() shouldBe false
        repo.getMeta("a.txt") shouldBe null
    }

    "a second write replaces the metadata" {
        val repo = repoOver(tempdir())
        repo.putFile(anna, "a.txt", "a", mapOf("title" to "first", "note" to "n"))

        repo.putFile(DepotSubjectId("customer-43"), "a.txt", "b", mapOf("title" to "second"))

        repo.getMeta("a.txt") shouldBe mapOf("title" to "second", "depot-subject" to "customer-43")
    }

    "the system subject is accepted by every repository, typed or not" {
        val repo = repoOver(tempdir())

        repo.putFile(DepotSubjectId.system, "sys.txt", "s")

        repo.getMeta("sys.txt") shouldBe mapOf("depot-subject" to "system")
    }
})
