package io.peekandpoke.ultra.codegen.ts

/**
 * Rewrites ROOT-RELATIVE module specifiers for a file emitted below the SDK root.
 *
 * Every shared specifier in the generator — `./models.ts`, `TsRuntime.Module.moduleSpecifier`, a
 * contributor's `importFrom` — is written as if the importing file sat at the SDK root, because for a
 * long time every emitted `.ts` did. Grouping the API clients under `api/` (2026-08-09) broke that
 * assumption: `./models.ts` from `api/authClient.ts` resolves to `api/models.ts`, which does not exist.
 *
 * This is the one place that adjustment happens, so a future `v2/` or per-feature directory is a
 * changed path rather than a hunt for hardcoded `./`.
 *
 * **This is arithmetic on a path, not a source transform.** The generator builds its own import lines
 * from a module string plus a name list, so the specifier is DATA. That is why this is safe where
 * rewriting `@sdk/…` inside hand-written `.vue` source was rejected as unsound — see
 * `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`. A path alias would remove the need
 * entirely and remains the long-term answer.
 */
object TsModulePaths {

    /**
     * [specifier] as seen from [fromFile], both relative to the SDK root.
     *
     * ```
     * rootRelative("./models.ts", fromFile = "authClient.ts")      // ./models.ts
     * rootRelative("./models.ts", fromFile = "api/authClient.ts")  // ../models.ts
     * ```
     *
     * A bare or package specifier (`zod`, `vue`) is returned untouched — those resolve from
     * `node_modules` and have no depth.
     */
    fun rootRelative(specifier: String, fromFile: String): String {
        if (!specifier.startsWith("./")) return specifier

        val depth = fromFile.replace('\\', '/').count { it == '/' }

        return if (depth == 0) specifier else "../".repeat(depth) + specifier.removePrefix("./")
    }
}
