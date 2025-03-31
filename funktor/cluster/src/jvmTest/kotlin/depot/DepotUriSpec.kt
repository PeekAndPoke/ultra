package de.peekandpoke.funktor.cluster.depot

import de.peekandpoke.funktor.cluster.depot.domain.DepotUri
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

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
                "depot://repo//file" to DepotUri(repo = "repo", path = "file"),
                "depot://repo/bucket" to DepotUri(repo = "repo", path = "bucket"),
                "depot://repo/bucket/" to DepotUri(repo = "repo", path = "bucket/"),
                "depot://repo/bucket/file" to DepotUri(repo = "repo", path = "bucket/file"),
                "depot://repo/bucket/file.jpg" to DepotUri(repo = "repo", path = "bucket/file.jpg"),
                "depot://repo/bucket/file/a.jpg" to DepotUri(repo = "repo", path = "bucket/file/a.jpg"),
                "depot://repo//bucket///file////a.jpg" to DepotUri(repo = "repo", path = "bucket/file/a.jpg"),
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
                DepotUri(repo = "/repo/", path = "bucket/file/a.jpg/") to "depot://repo/bucket/file/a.jpg/",
            ).forEach { (input, expected) ->

                "Uri of '$input' must be correct" {
                    val result = input.uri

                    result shouldBe expected
                }
            }
        }
    }
}
