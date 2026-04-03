package io.peekandpoke.kraft.addons.registry

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.KraftDsl

/**
 * Configures addons for the Kraft application.
 *
 * Usage:
 * ```
 * kraftApp {
 *     addons {
 *         signaturePad()
 *         marked(lazy = true)
 *     }
 * }
 * ```
 *
 * Non-lazy addons start loading immediately (via dynamic JS import).
 * Lazy addons defer loading until a component first subscribes to them.
 */
@KraftDsl
fun KraftApp.Builder.addons(block: AddonRegistryBuilder.() -> Unit) = apply {
    val registry = AddonRegistryBuilder().apply(block).build()
    setAttribute(AddonRegistry.key, registry)
    registry.loadEager()
}
