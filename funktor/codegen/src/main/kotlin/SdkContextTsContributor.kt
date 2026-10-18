package io.peekandpoke.funktor.codegen

import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.codegen.sdk.TsSdkEmitContext

/**
 * Ships `ui/sdkContext.ts` — how a page the ROUTER constructed reaches the app's `SdkConfig`.
 *
 * **Unconditional, and that is the whole point.** It used to ride along with
 * [InsightsTsContributor]'s `UI_FILES`, which made it appear and disappear with the insights
 * feature — while the line that consumes it, `provideSdkConfig(app, config)`, is HAND-WRITTEN in the
 * app's entry point, in a file the generator must never touch. An app that dropped insights, or
 * selected a profile without it, lost the module its own `main.ts` imports.
 *
 * That is the same argument `TsSdkBuilder` makes for emitting `mount.ts` and `styles.ts` even when
 * empty, and it applies more strongly here: EVERY contributed page needs this, not just insights'.
 * Found by `/feature-review`, 2026-08-09.
 *
 * Emitting it costs an SDK with no Vue pages one unused file. The alternative — deriving "some
 * contributor registered a page" — cannot help, because the app's import is unconditional either way.
 *
 * Lives in `funktor:codegen` rather than `ultra:codegen`'s runtime because it imports `vue`, and that
 * runtime is deliberately framework-neutral and type-checked by a toolchain with no Vue installed.
 */
class SdkContextTsContributor : TsSdkContributor {

    companion object {
        const val NAME: String = "funktor:sdk-context"

        /** Relative to the SDK root. Excluded from the barrel — see `TsBarrelEmitter`. */
        const val PATH: String = "ui/sdkContext.ts"
    }

    override val name: String = NAME

    override fun emit(context: TsSdkEmitContext) {
        // SHARED, not exclusive: `InsightsTsContributor` lists it among the primitives it depends on,
        // and any future page contributor will do the same. Identical content dedupes.
        context.out.sharedResource("ts/ui/sdkContext.ts", to = PATH)
    }
}
