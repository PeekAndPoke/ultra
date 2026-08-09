package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * One file on disk must be one entry in the plan, however a contributor spells its path.
 *
 * Keying on the raw string made `ui/theme.css` and `./ui/theme.css` two entries: the "written twice"
 * check never fired, and `writeTo` resolved both to the same file — last one in insertion order won,
 * silently. That defeats the exclusivity guarantee `sharedResource` depends on. Found by
 * `/feature-review`, 2026-08-09.
 */
class TsSdkOutputCanonicalPathSpec : FreeSpec() {

    init {
        "two spellings of one path are ONE entry" - {
            listOf("./ui/theme.css", "ui//theme.css", "ui/./theme.css").forEach { spelling ->
                "'$spelling' is the same entry as 'ui/theme.css'" {
                    val out = TsSdkOutput()

                    out.scopeFor("a").shared("ui/theme.css", ":root {}")
                    out.scopeFor("b").shared(spelling, ":root {}")

                    withClue("identical shared content dedupes — one file, not two") {
                        out.entries().count { it.path == "ui/theme.css" } shouldBe 1
                        out.entries().size shouldBe 1
                    }
                }
            }
        }

        "a differing SPELLING no longer hides an exclusivity conflict" {
            // The failure this closes: both contributors believe they own the file, and neither the
            // duplicate check nor the disk write noticed, because the two keys differed by two
            // characters.
            val out = TsSdkOutput()

            out.scopeFor("a").file("ui/theme.css", "from a")

            val thrown = shouldThrow<IllegalStateException> {
                out.scopeFor("b").file("./ui/theme.css", "from b")
            }

            thrown.message!! shouldContain "written twice"
        }

        "a differing SPELLING no longer hides a shared-content conflict" {
            val out = TsSdkOutput()

            out.scopeFor("a").shared("ui/theme.css", ":root { --a: 1 }")

            val thrown = shouldThrow<IllegalStateException> {
                out.scopeFor("b").shared("./ui/theme.css", ":root { --a: 2 }")
            }

            thrown.message!! shouldContain "DIFFERENT content"
        }
    }
}
