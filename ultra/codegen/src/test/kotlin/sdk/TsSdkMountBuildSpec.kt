package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.string.shouldContain as messageShouldContain
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.ts.TsMountEmitter
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

        "an SDK with no registered routes emits no mount.ts at all" {
            // An empty aggregate is noise: it would export an empty `routes` array and a `mountAll`
            // that does nothing, in every SDK that never contributes a page.
            val result = TsSdkBuilder.forTesting(
                listOf(Fx("fx:quiet") { })
            ).build()

            result.output.entries().map { it.path } shouldNotContain TsMountEmitter.PATH
        }
    }
}
