package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

/**
 * The arithmetic that lets an emitted file live below the SDK root.
 *
 * Every shared specifier in the generator is written root-relative. Emitting a client into `api/`
 * without this produces `./models.ts` from `api/authClient.ts`, which resolves to `api/models.ts` —
 * a file that does not exist, in output the consuming app cannot edit.
 */
class TsModulePathsSpec : FreeSpec() {

    init {
        "a file at the root is left alone" {
            TsModulePaths.rootRelative("./models.ts", fromFile = "models.ts") shouldBe "./models.ts"
            TsModulePaths.rootRelative("./runtime/client.ts", fromFile = "index.ts") shouldBe
                    "./runtime/client.ts"
        }

        "one level down climbs once" - {
            listOf(
                "./models.ts" to "../models.ts",
                "./runtime/client.ts" to "../runtime/client.ts",
                "./runtime/route.ts" to "../runtime/route.ts",
            ).forEach { (specifier, expected) ->
                "'$specifier' -> '$expected'" {
                    TsModulePaths.rootRelative(specifier, fromFile = "api/authClient.ts") shouldBe expected
                }
            }
        }

        "depth follows the path, however deep" {
            TsModulePaths.rootRelative("./models.ts", fromFile = "a/b/c/x.ts") shouldBe "../../../models.ts"
        }

        "a BARE specifier is untouched at any depth" {
            // `zod` and `vue` resolve from node_modules; prefixing one would break it, and a client
            // emits `import { z } from 'zod'` right next to the relative imports.
            withClue("a package specifier has no depth to compensate for") {
                TsModulePaths.rootRelative("zod", fromFile = "api/authClient.ts") shouldBe "zod"
                TsModulePaths.rootRelative("vue", fromFile = "a/b/x.ts") shouldBe "vue"
            }
        }

        "an already-climbing specifier is untouched" {
            // Not currently produced, but if a contributor ever declares one it must not be mangled
            // into `../../..`-nonsense — only `./`-rooted specifiers are this function's business.
            TsModulePaths.rootRelative("../outside.ts", fromFile = "api/x.ts") shouldBe "../outside.ts"
        }

        "backslashes count as separators" {
            // `TsSdkOutput.validatePath` accepts a Windows-style path; depth must not read as 0 there.
            TsModulePaths.rootRelative("./models.ts", fromFile = "api\\authClient.ts") shouldBe "../models.ts"
        }
    }
}
