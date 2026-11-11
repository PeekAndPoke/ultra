package io.peekandpoke.funktor.cluster.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.cluster.depot.repos.DepotInvalidPathException
import io.peekandpoke.funktor.cluster.depot.repos.fs.FileSystemRepository
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/** Every file system repository method refuses a path that leaves the root, like the S3 one (ULTRA-00304). */
class FileSystemRepositoryPathSpec : StringSpec({

    val root = tempdir()
    val outside = tempdir()
    val secret = File(outside, "secret.txt").apply { writeText("secret") }
    Files.createSymbolicLink(File(root, "link-out").toPath(), outside.toPath())

    val repo = object : FileSystemRepository("test", root.absolutePath) {}

    // A traversal and a symlink out of the root, for every method
    listOf("a/../../${outside.name}/secret.txt", "link-out/secret.txt").forEach { bad ->
        "listItems refuses $bad" { shouldThrow<DepotInvalidPathException> { repo.listItems(bad) } }
        "getFile refuses $bad" { shouldThrow<DepotInvalidPathException> { repo.getFile(bad) } }
        "getFolder refuses $bad" { shouldThrow<DepotInvalidPathException> { repo.getFolder(bad) } }
        "getContent refuses $bad" { shouldThrow<DepotInvalidPathException> { repo.getContent(bad) } }
        "putFile refuses $bad" { shouldThrow<DepotInvalidPathException> { repo.putFile(bad, "x".toByteArray()) {} } }
        "removeFile refuses $bad, the file outside stays" {
            shouldThrow<DepotInvalidPathException> { repo.removeFile(bad) }
            secret.readText() shouldBe "secret"
        }
    }

    "removeFile on a symlink inside the root removes the link, not the file it points to" {
        val target = File(root, "v7.txt").apply { writeText("data") }
        val link = File(root, "current.txt").toPath()
        Files.createSymbolicLink(link, target.toPath())

        repo.removeFile("current.txt") shouldBe true

        Files.exists(link, LinkOption.NOFOLLOW_LINKS) shouldBe false
        target.readText() shouldBe "data"
    }
})
