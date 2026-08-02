/**
 * How a MOUNTED page reaches the SDK.
 *
 * A page shipped by a Kotlin module is mounted by `mountAll(router)` as a bare route component — the
 * router constructs it, so nothing is there to pass props. But such a page needs an API client, and a
 * client needs the app's `baseUrl` and transport, which only the app knows. That is the gap this
 * closes: the app provides its config once, and any contributed page can reach it.
 *
 * ```ts
 * // main.ts
 * const app = createApp(App)
 * provideSdkConfig(app, config)
 * ```
 *
 * **Provide/inject rather than a module-level singleton**, because the SDK cannot construct a config:
 * `baseUrl` and the transport are the app's, and a generated module that reached for a global would
 * be untestable and would break the moment an app wanted two backends.
 *
 * **A component keeps its explicit prop.** This is a FALLBACK, not a replacement — `<InsightsPage
 * :client="…" />` still works and still wins, which is what lets a page be used outside a router, or
 * against a second config, or in a test with a stub.
 *
 * Lives with the Vue components rather than in `ultra:codegen`'s runtime on purpose: that runtime is
 * framework-neutral and is type-checked by a toolchain with no Vue installed, so a `vue` import there
 * would break its verification. This file is Vue-specific by nature.
 */
import { inject, type App, type InjectionKey } from 'vue'
import type { SdkConfig } from '../runtime/client.ts'

/** The injection key. Exported so an app can `app.provide` it directly if it prefers. */
export const SDK_CONFIG: InjectionKey<SdkConfig> = Symbol('funktor.sdk.config')

/**
 * Makes [config] available to every contributed page.
 *
 * Call once, on the app instance, before mounting. Uses `app.provide` rather than a component-level
 * `provide()` so it covers pages the router constructs — which is all of them.
 */
export function provideSdkConfig(app: App, config: SdkConfig): void {
    app.provide(SDK_CONFIG, config)
}

/**
 * The app's config, or `null` when it provided none.
 *
 * **Must be called during `setup()`**, like any `inject`. Returns `null` rather than throwing so a
 * component can prefer an explicit prop and only fall back to this — a throwing accessor would make
 * the prop unusable without a provider.
 */
export function useSdkConfigOrNull(): SdkConfig | null {
    return inject(SDK_CONFIG, null)
}
