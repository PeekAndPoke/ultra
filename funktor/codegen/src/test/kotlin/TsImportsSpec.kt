package io.peekandpoke.funktor.codegen

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class TsImportsSpec : FreeSpec() {

    init {
        "finds every import form the emitted files use" {
            val content = """
                <script setup lang="ts">
                import { computed, ref } from 'vue'
                import type { Ref } from 'vue'
                import {
                    readKontainer,
                    readVault,
                } from './slices.ts'
                import './insights.css'
                export * from "../models.ts"
                export { a } from './a.ts'

                async function load() {
                    return await import('vis-network/standalone')
                }
                </script>
            """.trimIndent()

            TsImports.of(content) shouldBe setOf(
                "vue", "./slices.ts", "./insights.css", "../models.ts", "./a.ts", "vis-network/standalone",
            )
        }

        "ignores imports that only appear in comments" {
            val content = """
                /**
                 * ```ts
                 * import { FunktorInsightsClient } from '@sdk'
                 * ```
                 * or `import('./lazy.ts')` in prose
                 */
                // import x from 'commented-out'
                <!-- import y from 'in-a-template-comment' -->
                export const KEY = Symbol('funktor.sdk.config')
            """.trimIndent()

            TsImports.of(content) shouldBe emptySet()
        }

        "a string after export is not an import" {
            TsImports.of("export const label = 'vue'\nexport function f() { return \"x\" }") shouldBe emptySet()
        }

        "relative means ./ or ../ and nothing else" {
            TsImports.isRelative("./a.ts") shouldBe true
            TsImports.isRelative("../ui/JsonTree.vue") shouldBe true
            TsImports.isRelative("vue") shouldBe false
            TsImports.isRelative("@vue/runtime-core") shouldBe false
        }

        "the package of a specifier keeps a scope and drops a subpath" {
            TsImports.packageOf("vis-network/standalone") shouldBe "vis-network"
            TsImports.packageOf("@vue/runtime-core/dist/x.js") shouldBe "@vue/runtime-core"
            TsImports.packageOf("zod") shouldBe "zod"
        }
    }
}
