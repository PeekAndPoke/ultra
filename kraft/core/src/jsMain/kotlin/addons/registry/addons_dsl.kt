package io.peekandpoke.kraft.addons.registry

import io.peekandpoke.kraft.KraftApp

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
 * Lazy addons defer loading until they are first read or subscribed to.
 */
fun KraftApp.Builder.addons(block: AddonRegistryBuilder.() -> Unit) = apply {
    val registry = AddonRegistryBuilder().apply(block).build()
    setAttribute(AddonRegistry.key, registry)
    registry.loadEager()
}
