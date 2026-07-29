package io.peekandpoke.ultra.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

/** Regressions for the JVM-only half of the 2026-07-29 backlog sweep. */
class CommonFixesJvmSpec : StringSpec({

    // toUri ///////////////////////////////////////////////////////////////////////////////////////

    "toUri keeps a fragment at the end, behind the query" {
        "http://x/page#frag".toUri("a" to "b") shouldBe "http://x/page?a=b#frag"
        "http://x/page?q=1#frag".toUri("a" to "b") shouldBe "http://x/page?q=1&a=b#frag"
    }

    "toUri preserves repeated keys" {
        "http://x".toUri(listOf("tag" to "a", "tag" to "b")) shouldBe "http://x?tag=a&tag=b"
    }

    "toUri does not double a trailing separator" {
        "http://x/page?".toUri("a" to "b") shouldBe "http://x/page?a=b"
        "http://x/page?q=1&".toUri("a" to "b") shouldBe "http://x/page?q=1&a=b"
    }

    "toUri leaves the string alone when there is nothing to add" {
        "http://x".toUri(emptyMap()) shouldBe "http://x"
    }

    "toUri form-encodes keys and values" {
        "http://x".toUri("a b" to "c&d") shouldBe "http://x?a+b=c%26d"
    }

    // classes.kt //////////////////////////////////////////////////////////////////////////////////

    "getRelativePackagePath handles a class without a package" {
        // an array class has a null package - this used to NPE
        ByteArray::class.getRelativePackagePath(String::class)
        String::class.getRelativePackagePath(ByteArray::class)
    }

    // files.kt ////////////////////////////////////////////////////////////////////////////////////

    "child rejects a path that escapes the parent" {
        val base = Files.createTempDirectory("ultra-child").toFile()

        base.child(File("a/b")).path shouldBe File(base, "a/b").path

        shouldThrow<IllegalArgumentException> { base.child(File("../outside")) }
        shouldThrow<IllegalArgumentException> { base.child(File("a/../../outside")) }
        shouldThrow<IllegalArgumentException> { base.child(File("/absolute")) }

        // a `..` that stays inside is fine
        base.child(File("a/../b")).path shouldBe File(base, "b").path

        base.deleteRecursively()
    }

    "ensureDirectory reports a failure instead of returning quietly" {
        val dir = Files.createTempDirectory("ultra-ensure").toFile()
        val asFile = File(dir, "iamafile").apply { writeText("x") }

        shouldThrow<IllegalStateException> { asFile.ensureDirectory() }

        // the happy paths still work
        File(dir, "fresh").ensureDirectory().isDirectory shouldBe true
        dir.ensureDirectory().isDirectory shouldBe true

        dir.deleteRecursively()
    }

    "cleanDirectory does not follow symlinks out of the tree" {
        val root = Files.createTempDirectory("ultra-clean").toFile()
        val outside = Files.createTempDirectory("ultra-outside").toFile()
        val treasure = File(outside, "keep-me.txt").apply { writeText("precious") }

        val target = File(root, "sub").apply { mkdirs() }
        File(target, "inside.txt").writeText("gone")
        Files.createSymbolicLink(File(root, "link").toPath(), outside.toPath())

        root.cleanDirectory()

        root.isDirectory shouldBe true
        root.listFiles()!!.size shouldBe 0

        // the link is gone, but what it pointed at survives
        treasure.exists() shouldBe true
        treasure.readText() shouldBe "precious"

        outside.deleteRecursively()
        root.deleteRecursively()
    }
})
