package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder

class AuthTsContributorSpec : FreeSpec() {

    init {
        "it ships the session runtime alongside a REST client" {
            // The point of the test: BOTH contributors require `runtime/http.ts`. Planned with
            // `out.file` that is a hard error; with `out.shared` the identical content dedupes.
            val result = TsSdkBuilder.forTesting(
                listOf(
                    RestApiTsContributor(lazyOf(listOf(FxDemoApiFeature(listOf(FxTalksApiRoutes()))))),
                    AuthTsContributor(),
                )
            ).build()

            val paths = result.output.entries().map { it.path }

            paths shouldContain "runtime/auth.ts"

            withClue("http.ts is required by both and must appear exactly once") {
                paths.count { it == "runtime/http.ts" } shouldBe 1
            }

            withClue("the barrel re-exports it, so `import { AuthSession } from '@sdk'` works") {
                result.output.entries().single { it.path == "index.ts" }
                    .content shouldContain "export * from './runtime/auth.ts'"
            }
        }

        "an auth-ONLY SDK is refused, and that is correct" {
            // The session runtime contributes no root TYPES — it is behaviour, not a model. So an SDK
            // built from it alone has nothing to walk, and the builder's empty-SDK guard fires. Worth
            // pinning: it is why the closure test above needs a REST contributor beside it, and it
            // would otherwise look like a bug in this contributor the first time someone hits it.
            val thrown = runCatching {
                TsSdkBuilder.forTesting(listOf(AuthTsContributor())).build()
            }.exceptionOrNull()

            thrown!!.message!! shouldContain "supplied any root type"
        }
    }
}
