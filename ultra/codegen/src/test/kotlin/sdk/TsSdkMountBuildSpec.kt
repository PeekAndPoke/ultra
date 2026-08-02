package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.string.shouldContain as messageShouldContain
import io.kotest.matchers.string.shouldNotContain as shouldNotContainText
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.ts.TsMountEmitter
import io.peekandpoke.ultra.codegen.ts.TsStylesEmitter
import kotlin.reflect.typeOf

/**
 * The registry as the BUILDER drives it — the half `TsSdkRegistrySpec` cannot reach, because the
 * cross-check between registered routes and emitted files is the only place both are visible.
 */
class TsSdkMountBuildSpec : FreeSpec() {

    /** Roots one type so the SDK is not empty, then does whatever [emit] says. */
    private class Fx(
        override val name: String,
        private val emit: (TsSdkEmitContext) -> Unit,
    ) : TsSdkContributor {
        override fun contribute(roots: TsSdkRoots) = roots.root(typeOf<FxTalk>(), "$name:root")
        override fun emit(context: TsSdkEmitContext) = emit.invoke(context)
    }

    init {
        "a registered route whose component was never emitted is a hard error" {
            // Without this the component surfaces as a module-resolution error inside GENERATED
            // output — naming a file, not the contributor that asked for it, and only once someone
            // builds the consuming app.
            val thrown = shouldThrow<IllegalStateException> {
                TsSdkBuilder.forTesting(
                    listOf(
                        Fx("fx:forgetful") { ctx ->
                            ctx.registry.route("/oops", "pages/Missing.vue", requiresAuth = true)
                        }
                    )
                ).build()
            }

            withClue("the message must name the path, the component AND the contributor") {
                thrown.message!! messageShouldContain "/oops"
                thrown.message!! messageShouldContain "pages/Missing.vue"
                thrown.message!! messageShouldContain "fx:forgetful"
            }
        }

        "a route whose component IS emitted builds, and mount.ts appears" {
            val result = TsSdkBuilder.forTesting(
                listOf(
                    Fx("fx:tidy") { ctx ->
                        ctx.out.file("pages/Ok.vue", "<template><div/></template>")
                        ctx.registry.route("/ok", "pages/Ok.vue", requiresAuth = false)
                    }
                )
            ).build()

            val paths = result.output.entries().map { it.path }

            paths shouldContain TsMountEmitter.PATH

            withClue("and the barrel re-exports it, so `import { mountAll } from '@sdk'` works") {
                result.output.entries().single { it.path == "index.ts" }
                    .content messageShouldContain "export * from './mount.ts'"
            }
        }

        "an SDK with no registered routes STILL emits mount.ts" {
            // REVERSED on 2026-08-02. This used to assert the opposite, reasoning that an empty
            // aggregate is noise — an empty `routes` array and a `mountAll` that does nothing.
            //
            // That reasoning weighed the wrong cost. `import { mountAll } from './funktorsdk/mount.ts'`
            // is a HAND-WRITTEN line in the app, in a file the generator must never touch. Emitting
            // conditionally makes that line compile or not depending on whether some contributor
            // elsewhere happened to register a page — so removing the last page contributor, or
            // selecting a profile without one, breaks the app's wiring with a module-resolution error
            // rather than an empty menu. An SDK surface an app cannot rely on being there is worse
            // than nine wasted lines.
            val result = TsSdkBuilder.forTesting(
                listOf(Fx("fx:quiet") { })
            ).build()

            val mount = result.output.entries().single { it.path == TsMountEmitter.PATH }

            withClue("and it is a usable empty table, not a stub") {
                mount.content messageShouldContain "export const routes"
                mount.content messageShouldContain "export function mountAll"
            }
        }

        "styles.ts is emitted too, and is likewise unconditional" {
            val result = TsSdkBuilder.forTesting(
                listOf(Fx("fx:quiet") { })
            ).build()

            val styles = result.output.entries().single { it.path == TsStylesEmitter.PATH }

            withClue("an empty stylesheet aggregate must still be a MODULE under isolatedModules") {
                styles.content messageShouldContain "export {}"
            }
        }

        "a registered stylesheet nobody emitted is a hard error" {
            val thrown = shouldThrow<IllegalStateException> {
                TsSdkBuilder.forTesting(
                    listOf(
                        Fx("fx:forgetful-css") { ctx -> ctx.registry.style("ui/theme.css", order = 0) }
                    )
                ).build()
            }

            withClue("the message must name the sheet AND the contributor") {
                thrown.message!! messageShouldContain "ui/theme.css"
                thrown.message!! messageShouldContain "fx:forgetful-css"
            }
        }

        "contributed stylesheets are imported in cascade order, whatever order contributors ran in" {
            // The property that matters: a theme defines the custom properties a feature sheet
            // consumes, so the wrong order does not error — the overrides silently lose.
            val result = TsSdkBuilder.forTesting(
                listOf(
                    Fx("fx:feature") { ctx ->
                        ctx.out.file("feature/feature.css", ".fk-feature { color: var(--fk-fg) }")
                        ctx.registry.style("feature/feature.css", order = 100)
                    },
                    Fx("fx:theme") { ctx ->
                        ctx.out.file("ui/theme.css", ":root { --fk-fg: black }")
                        ctx.registry.style("ui/theme.css", order = 0)
                    },
                )
            ).build()

            val styles = result.output.entries().single { it.path == TsStylesEmitter.PATH }.content

            withClue("the theme must precede the feature sheet despite being contributed second") {
                styles.indexOf("./ui/theme.css") shouldBeLessThan styles.indexOf("./feature/feature.css")
            }

            withClue("and the barrel must NOT re-export it — a type-only import would pull in CSS") {
                result.output.entries().single { it.path == "index.ts" }
                    .content shouldNotContainText "styles.ts"
            }
        }
    }
}
