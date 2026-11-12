package io.peekandpoke.ultra.depot

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.domain.DepotUri

class DepotUriSpec : FreeSpec() {

    init {

        ////  PARSING  /////////////////////////////////////////////////////////////////////////////////////////////////

        "Parsing" - {
            listOf(
                "" to null,
                "repo" to null,
                "repo/bucket" to null,
                "repo/bucket/file" to null,
                "depot://" to null,
                "depot://repo" to null,
                "depot:///bucket/file" to null,
                "depot:// repo/file" to null,
                "depot://repo//file" to DepotUri(repo = "repo", path = "/file"),
                "depot://repo/a b.pdf " to DepotUri(repo = "repo", path = "a b.pdf "),
                "depot://repo/a\\b.pdf" to DepotUri(repo = "repo", path = "a\\b.pdf"),
                "depot://repo/" to DepotUri(repo = "repo", path = ""),
                "depot://repo/bucket" to DepotUri(repo = "repo", path = "bucket"),
                "depot://repo/bucket/" to DepotUri(repo = "repo", path = "bucket/"),
                "depot://repo/bucket/file" to DepotUri(repo = "repo", path = "bucket/file"),
                "depot://repo/bucket/file.jpg" to DepotUri(repo = "repo", path = "bucket/file.jpg"),
                "depot://repo/bucket/file/a.jpg" to DepotUri(repo = "repo", path = "bucket/file/a.jpg"),
                "depot://repo//bucket///file////a.jpg" to DepotUri(repo = "repo", path = "/bucket///file////a.jpg"),
            ).forEach { (input, expected) ->

                "Parsing '$input' must work" {
                    val result = DepotUri.parse(input)

                    result shouldBe expected
                }
            }
        }

        ////  URI  /////////////////////////////////////////////////////////////////////////////////////////////////

        "DepotUri.uri" - {
            listOf(
                DepotUri(repo = "repo", path = "bucket/file") to "depot://repo/bucket/file",
                DepotUri(repo = "repo", path = "bucket/file/a.jpg") to "depot://repo/bucket/file/a.jpg",
                DepotUri(repo = "repo", path = "bucket/file/a.jpg/") to "depot://repo/bucket/file/a.jpg/",
            ).forEach { (input, expected) ->

                "Uri of '$input' must be correct" {
                    val result = input.uri

                    result shouldBe expected
                }
            }
        }

        "A path is kept as it is: paths that differ have uris that differ, and read back" {
            val paths = listOf("shared/report.pdf", "shared/report.pdf ", " shared/report.pdf", "shared\\report.pdf", "shared//report.pdf", "/shared/report.pdf")

            paths.map { DepotUri(repo = "repo", path = it).uri }.toSet().size shouldBe paths.size

            paths.forEach { path ->
                DepotUri.parse(DepotUri(repo = "repo", path = path).uri) shouldBe DepotUri(repo = "repo", path = path)
            }
        }

        "A repository name with a slash, a backslash or blanks around it is refused" {
            listOf("", " ", "/repo/", "docs/eu", "docs\\eu", " repo", "repo ").forEach { name ->
                shouldThrow<IllegalArgumentException> { DepotUri(repo = name, path = "file") }
            }
        }
    }
}
