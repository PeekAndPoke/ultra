package io.peekandpoke.ultra.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.repos.DepotInvalidPathException
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
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

    "removeFile with an empty path leaves a root that is a link alone" {
        val volume = tempdir()
        File(volume, "data.txt").writeText("data")
        val holder = tempdir()
        val linkedRoot = File(holder, "depot").toPath()
        Files.createSymbolicLink(linkedRoot, volume.toPath())

        val linked = object : FileSystemRepository("test", linkedRoot.toString()) {}

        linked.removeFile("") shouldBe false
        linked.removeFile("/") shouldBe false

        Files.isSymbolicLink(linkedRoot) shouldBe true
        File(volume, "data.txt").readText() shouldBe "data"
    }

    "removeFile on a link to a folder removes nothing" {
        val folder = File(root, "v7").apply { mkdirs() }
        File(folder, "a.txt").writeText("a")
        val link = File(root, "current").toPath()
        Files.createSymbolicLink(link, folder.toPath())

        repo.removeFile("current") shouldBe false

        Files.isSymbolicLink(link) shouldBe true
        File(folder, "a.txt").readText() shouldBe "a"
    }

    "items reached through a link inside the root keep the path they were asked for" {
        val target = File(root, "v42.json").apply { writeText("{}") }
        Files.createSymbolicLink(File(root, "latest.json").toPath(), target.toPath())
        val day = File(root, "archive/day").apply { mkdirs() }
        File(day, "r.json").writeText("{}")
        Files.createSymbolicLink(File(root, "records-2026-07-31").toPath(), day.toPath())

        repo.getFile("latest.json")!!.path shouldBe "latest.json"
        repo.putFile("latest.json", "{}".toByteArray()) {}.path shouldBe "latest.json"
        repo.listItems("records-2026-07-31").map { it.path } shouldBe listOf("records-2026-07-31/r.json")
    }

    "a name the platform cannot encode is refused, not an error" {
        shouldThrow<DepotInvalidPathException> { repo.getContent("a${Char(0xD800)}.json") }
    }

    "getMeta refuses the same paths" {
        shouldThrow<DepotInvalidPathException> { repo.getMeta("a/../../x") }
    }

    "removeFile never unlinks a link outside the root, reached through a link out and back in" {
        val outside = tempdir()
        val data = File(root, "data.txt").apply { writeText("data") }
        val back = File(outside, "back.txt").toPath()
        Files.createSymbolicLink(back, data.toPath())
        Files.createSymbolicLink(File(root, "out").toPath(), outside.toPath())

        repo.removeFile("out/back.txt")

        // The link outside the root is still there
        Files.isSymbolicLink(back) shouldBe true
    }

    "an item reached through a link keeps the link's name" {
        val target = File(root, "v43.json").apply { writeText("{}") }
        Files.createSymbolicLink(File(root, "newest.json").toPath(), target.toPath())

        repo.getFile("newest.json")!!.name shouldBe "newest.json"
    }
})
