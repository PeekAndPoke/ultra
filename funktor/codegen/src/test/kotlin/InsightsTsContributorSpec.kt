package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain as shouldContainText
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.invariantSeparatorsPathString

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

        /** Where [specifier], imported by the emitted file at [from], lands inside the SDK. */
        private fun resolveImport(from: String, specifier: String): String {
            val dir = from.substringBeforeLast('/', missingDelimiterValue = "")
            return Paths.get(dir, specifier).normalize().invariantSeparatorsPathString
        }
    }

    private fun assertMirrors(dirName: String, declared: List<String>) {
        val dir = resourceDir(dirName)

        withClue("$dir must exist — the contributor emits from it") {
            dir.isDirectory shouldBe true
        }

        // RECURSIVE. `listFiles()` does not descend, so a tab added under `ts/insights/tabs/`
        // would satisfy this spec while the generator never emitted it — the exact 2026-08-02
        // drift, one directory down, and just as invisible to `vue-tsc`.
        val onDisk = dir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(dir).invariantSeparatorsPath }
            .toSet()

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

        //  The imports BETWEEN the emitted files  /////////////////////////////////////////////////////
        //
        //  I4 of the 2026-08-24 gate: the mirror checks above guard the LISTS, not what the files
        //  import. Rename a `ui/` file and update `UI_FILES`, miss one importing tab, and both set
        //  comparisons pass while `vite build` fails downstream. Run against the REAL feature, so the
        //  runtime and model files the pages import are emitted the way an app gets them.

        "with the REAL insights feature" - {

            val output = RealInsightsSdk.build()
            val paths = output.entries().map { it.path }.toSet()
            val sources = output.entries().filter { it.path.endsWith(".ts") || it.path.endsWith(".vue") }

            "every relative import of every emitted file names an emitted file" {
                val imports = sources.flatMap { entry ->
                    TsImports.of(entry.content).filter { TsImports.isRelative(it) }.map { entry.path to it }
                }

                withClue("the walk must have something to walk, or the check below is vacuous") {
                    imports.size shouldBeGreaterThan 50
                }

                val dangling = imports
                    .map { (from, specifier) -> "$from -> $specifier" to resolveImport(from, specifier) }
                    .filter { (_, target) -> target !in paths }
                    .map { it.first }

                withClue("imports whose target the SDK does not contain -- `vue-tsc` cannot see these, `vite build` can") {
                    dangling.shouldBeEmpty()
                }
            }

            "a .ts file of the insights UI imports no package but vue" {
                // The SDK barrel re-exports every `.ts` file, so a package imported by one becomes a
                // dependency of every app importing the barrel. `vis-network` is loaded by
                // `NetworkGraph.vue`, which only the tabs reach.
                val packages = sources
                    .filter { it.path.endsWith(".ts") && (it.path.startsWith("insights/") || it.path.startsWith("ui/")) }
                    .flatMap { entry -> TsImports.of(entry.content).filterNot { TsImports.isRelative(it) } }
                    .toSet()

                packages shouldBe setOf("vue")
            }

            "the graph library is imported by the graph component and by nothing else" {
                sources.filter { entry -> TsImports.of(entry.content).any { it.startsWith("vis-network") } }
                    .map { it.path } shouldBe listOf("insights/NetworkGraph.vue")
            }
        }

        //  What emit() actually DOES  ///////////////////////////////////////////////////////////////
        //
        //  Added by /feature-review 2026-08-09. Nothing had ever RUN emit() with the feature present,
        //  so every decision inside it was unasserted — three mutations survived: swapping the two
        //  cascade orders, deleting the client-name check, and deleting the route registration.

        "with the insights feature present" - {

            fun emitted() = TsSdkBuilder
                .forTesting(
                    listOf(
                        RestApiTsContributor(lazyOf(listOf(FxInsightsApiFeature()))),
                        InsightsTsContributor(lazyOf(listOf(FxInsightsApiFeature()))),
                    )
                )
                .build()

            "the theme is loaded BEFORE the insights sheet" {
                // THE assertion this whole ordering mechanism exists for, and the one pairing that
                // actually exists in the repo. `insights.css` consumes custom properties `theme.css`
                // defines, so the wrong order does not error — the overrides silently stop applying.
                // Swapping the two order constants used to leave every test green.
                val styles = emitted().output.entries()
                    .single { it.path == "styles.ts" }
                    .content

                styles.indexOf("./ui/theme.css") shouldBeLessThan styles.indexOf("./insights/insights.css")
            }

            "the page is registered as a route, with a nav entry" {
                val mount = emitted().output.entries().single { it.path == "mount.ts" }.content

                mount shouldContainText "'/insights'"
                mount shouldContainText "insights/InsightsPage.vue"
                mount shouldContainText "'Insights'"
            }

            "the nav entry gates on the route the page cannot work without" {
                // Before this, the menu offered "Insights" to every signed-in operator and the page
                // 403'd on every call — `InsightsApi` floors at `isSuperUser()` while `requiresAuth`
                // is true for any session at all.
                val mount = emitted().output.entries().single { it.path == "mount.ts" }.content

                withClue("method, PATTERN and publicness, all read off the live route graph") {
                    mount shouldContainText
                            "{ method: 'GET', uri: '/_/funktor/insights/records', isPublic: false },"
                }
            }

            "every declared file is actually emitted" {
                val paths = emitted().output.entries().map { it.path }.toSet()

                InsightsTsContributor.UI_FILES.forEach { paths shouldContain "ui/$it" }
                InsightsTsContributor.INSIGHTS_FILES.forEach { paths shouldContain "insights/$it" }
            }
        }

        "with NO insights feature, nothing insights-shaped is emitted" {
            val paths = TsSdkBuilder
                .forTesting(
                    listOf(
                        RestApiTsContributor(lazyOf(listOf(FxDemoApiFeature(listOf(FxTalksApiRoutes()))))),
                        InsightsTsContributor(lazyOf(listOf(FxDemoApiFeature(listOf(FxTalksApiRoutes()))))),
                    )
                )
                .build().output.entries().map { it.path }

            paths.none { it.startsWith("insights/") } shouldBe true

            withClue("and no /insights route is registered either") {
                paths.single { it == "mount.ts" }
                TsSdkBuilder.forTesting(
                    listOf(
                        RestApiTsContributor(lazyOf(listOf(FxDemoApiFeature(listOf(FxTalksApiRoutes()))))),
                        InsightsTsContributor(lazyOf(listOf(FxDemoApiFeature(listOf(FxTalksApiRoutes()))))),
                    )
                ).build().output.entries()
                    .single { it.path == "mount.ts" }.content shouldNotContain "/insights"
            }
        }

        "a page whose client the PROFILE filtered out fails the build, naming the page" {
            // The HIGH from /feature-review 2026-08-09. The feature being INSTALLED and its client
            // being EMITTED are different things: a profile filters routes, and RestApiTsContributor
            // writes no client for a feature whose routes were all excluded. The pages import that
            // client, so without the `requires` declaration this shipped an SDK that cannot resolve
            // its own imports — invisible to `vue-tsc`, and only `vite build` would say so.
            val thrown = runCatching {
                val features = listOf(FxDemoApiFeature(listOf(FxTalksApiRoutes())), FxInsightsApiFeature())

                TsSdkBuilder.forTesting(
                    listOf(
                        // A profile that keeps the demo feature but admits nothing from insights.
                        // The second feature matters: filtering EVERY route leaves the SDK with no
                        // roots at all, and the builder rejects that first — a different error, and
                        // not the one this test is about.
                        RestApiTsContributor(
                            lazyOf(features),
                            include = { route -> !route.pattern.pattern.startsWith("/_/funktor/insights") },
                        ),
                        InsightsTsContributor(lazyOf(features)),
                    )
                ).build()
            }.exceptionOrNull()

            thrown!!.message!! shouldContainText "/insights"
            thrown.message!! shouldContainText "api/funktorInsightsClient.ts"

            withClue("the message must say what to do about it") {
                thrown.message!! shouldContainText "profile"
            }
        }

        "renaming the gated endpoint fails the build instead of hiding the menu entry" {
            // THE test that a literal `ApiRouteRef` would not survive. A hand-written method/uri pair
            // sails past a rename and ships a nav entry gated on a route the server no longer serves
            // — `ApiAcl` answers Denied for it, because absence IS denial, so the entry vanishes for
            // every user and nothing anywhere says why.
            val thrown = runCatching {
                TsSdkBuilder.forTesting(
                    listOf(
                        RestApiTsContributor(lazyOf(listOf(FxInsightsRenamedApiFeature()))),
                        InsightsTsContributor(lazyOf(listOf(FxInsightsRenamedApiFeature()))),
                    )
                ).build()
            }.exceptionOrNull()

            // QUOTED. Bare "listRecords" is a substring of "listRecordsV2", so it was satisfied by
            // the known-endpoints list alone — the assertion passed without the message ever naming
            // the member that broke. Caught in the 2026-08-24 gate; exactly the vacuous-assertion
            // shape CLAUDE.md warns about.
            thrown!!.message!! shouldContainText "'listRecords'"

            withClue("and must offer the names that DO exist") {
                thrown.message!! shouldContainText "listRecordsV2"
            }
        }
    }
}
