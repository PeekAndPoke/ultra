package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Guards the one thing about [InsightsTsContributor] that cannot fail loudly on its own: its file lists
 * are a MANUAL MIRROR of two resource directories, and nothing else notices when they drift.
 *
 * This is not hypothetical. On 2026-08-02 four collector tabs were added under `ts/insights/`,
 * `InsightsDetailPage.vue` imported all four, and the lists were not updated — so the generator emitted
 * a page importing four files it had never written.
 *
 * **The reason that shipped is worth remembering: `vue-tsc` cannot catch it.** The consuming app's
 * `shims-vue.d.ts` declares `module '*.vue'`, a wildcard that resolves ANY `.vue` specifier whether the
 * file exists or not — so the typecheck was clean while the app was broken. Only `vite build` failed, and
 * only if someone ran it. This spec moves the failure back to where the mistake is made.
 */
class InsightsTsContributorSpec : FreeSpec() {

    companion object {
        /**
         * Resolved from the module root rather than the classpath.
         *
         * The classpath copy is what the contributor reads at runtime, but it is a BUILD OUTPUT — if the
         * source directory gained a file and the build did not re-run, the classpath would agree with the
         * stale list and the spec would pass while the source disagreed. The source tree is the thing the
         * lists are supposed to mirror.
         */
        private fun resourceDir(name: String): File =
            File("src/main/resources/ts/$name").absoluteFile
    }

    private fun assertMirrors(dirName: String, declared: List<String>) {
        val dir = resourceDir(dirName)

        withClue("$dir must exist — the contributor emits from it") {
            dir.isDirectory shouldBe true
        }

        val onDisk = dir.listFiles().orEmpty().filter { it.isFile }.map { it.name }.toSet()

        withClue(
            "files under ts/$dirName that the contributor never emits. A page importing one of these " +
                    "produces an SDK that cannot resolve its own imports, and `vue-tsc` will NOT say so " +
                    "— add them to InsightsTsContributor"
        ) {
            (onDisk - declared.toSet()).shouldBeEmpty()
        }

        withClue("files the contributor claims to emit but which do not exist — `out.resource` throws") {
            (declared.toSet() - onDisk).shouldBeEmpty()
        }
    }

    init {
        "the ui file list mirrors ts/ui exactly" {
            assertMirrors("ui", InsightsTsContributor.UI_FILES)
        }

        "the insights file list mirrors ts/insights exactly" {
            assertMirrors("insights", InsightsTsContributor.INSIGHTS_FILES)
        }

        "the four tabs whose omission caused this spec to exist are listed" {
            // Pinned by name, not just by the mirror check: the mirror passes vacuously if someone
            // deletes the resources AND the list entries together, which is the shape of a bad rebase.
            listOf("RuntimeTab.vue", "VaultTab.vue", "KontainerTab.vue", "AppConfigTab.vue").forEach {
                InsightsTsContributor.INSIGHTS_FILES shouldContain it
            }
        }
    }
}
