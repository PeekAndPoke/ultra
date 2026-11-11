package io.peekandpoke.ultra.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.DepotInvalidPathException
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository
import io.peekandpoke.ultra.depot.repos.validateDepotPath
import java.io.File
import java.nio.file.Files

/** Paths in a [FileSystemRepository] and the shared depot path check (ULTRA-00304). */
class FileSystemRepositorySpec : StringSpec({

    fun repoOver(dir: File) = object : FileSystemRepository<Any>("test", dir.absolutePath) {}

    "the depot path check refuses dot segments and NUL, and allows the rest" {
        listOf("..", "a/../b", "./a", "a/.", "a\\..\\b", "..\\x", "a${Char(0)}b").forEach { path ->
            shouldThrow<DepotInvalidPathException> { path.validateDepotPath() }
        }

        // A backslash elsewhere is allowed: S3 keys may hold one
        listOf("", "/", "a/b.json", "records-2026-01-01/x.json", "a..b", "/abs/path", "C:\\scan.pdf").forEach { path ->
            path.validateDepotPath() shouldBe path
        }
    }

    "listItems with a limit lists the folder it was given" {
        val dir = tempdir()
        File(dir, "sub").mkdirs()
        File(dir, "sub/a.txt").writeText("a")
        File(dir, "top.txt").writeText("t")

        repoOver(dir).listItems(path = "sub", limit = 10).map { it.name } shouldContainExactly listOf("a.txt")
    }

    "an entry whose name holds two dots is listed, not a failure of the folder" {
        val dir = tempdir()
        File(dir, "a..b.txt").writeText("x")
        File(dir, "c.txt").writeText("y")

        repoOver(dir).listItems("").map { it.name }.sorted() shouldContainExactly listOf("a..b.txt", "c.txt")
    }

    "an absolute path stays inside the repository" {
        val dir = tempdir()
        File(dir, "etc").mkdirs()
        File(dir, "etc/hosts").writeText("inside")

        String(repoOver(dir).getContent("/etc/hosts")!!.getContentBytes()) shouldBe "inside"
    }

    "a symlink inside the repository that points out of it is neither read nor listed" {
        val dir = tempdir()
        val outside = tempdir()
        File(outside, "secret.txt").writeText("secret")
        Files.createSymbolicLink(File(dir, "link-out").toPath(), outside.toPath())
        File(dir, "inside.txt").writeText("x")

        val repo = repoOver(dir)

        shouldThrow<DepotInvalidPathException> { repo.getContent("link-out/secret.txt") }
        repo.listItems("").map { it.name } shouldContainExactly listOf("inside.txt")
    }

    "a listed path goes back into the repository, on every platform /-separated" {
        val dir = tempdir()
        File(dir, "sub").mkdirs()
        File(dir, "sub/a.txt").writeText("nested")

        val repo = repoOver(dir)
        val listed = repo.listItems("sub").single()

        listed.path shouldBe "sub/a.txt"
        String(repo.getContent(listed.path)!!.getContentBytes()) shouldBe "nested"
    }

    "a dangling symlink is refused, so a write cannot land outside the root" {
        val dir = tempdir()
        val outside = tempdir()
        val target = File(outside, "not-yet.txt")
        Files.createSymbolicLink(File(dir, "dangling").toPath(), target.toPath())

        shouldThrow<DepotInvalidPathException> { repoOver(dir).putFile(DepotSubjectId.system, "dangling", "x".toByteArray()) }

        target.exists() shouldBe false
    }
})
