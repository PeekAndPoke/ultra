package de.peekandpoke.ktorfx.staticweb.resources.prismjs

import de.peekandpoke.ktorfx.core.model.CacheBuster
import de.peekandpoke.ktorfx.staticweb.resources.WebResourceGroup
import de.peekandpoke.ktorfx.staticweb.resources.WebResources
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

/**
 * Imports the [KtorFX_PrismJs] into the [KontainerBuilder]
 */
fun KontainerBuilder.ktorFxPrismJs() = module(KtorFX_PrismJs)

val WebResources.prismJs get() = get(PrismJsWebResources::class)

/**
 * Defines the prism js module
 */
val KtorFX_PrismJs = module {
    singleton(PrismJsWebResources::class)
}

/**
 * Defines the web resources used by prism js
 */
class PrismJsWebResources(cacheBuster: CacheBuster) : WebResourceGroup(cacheBuster, {
    webjarCss("/vendor/prismjs/prism.css")
    webjarCss("/vendor/prismjs/prism.css")
    webjarCss("/vendor/prismjs/plugins/toolbar/prism-toolbar.css")

    webjarJs("/vendor/prismjs/prism.js")
    webjarJs("/vendor/prismjs/plugins/toolbar/prism-toolbar.js")
    webjarJs("/vendor/prismjs/show-language/prism-show-language.js")
    webjarJs("/vendor/prismjs/components/prism-markup.js")
    webjarJs("/vendor/prismjs/components/prism-json.js")
    webjarJs("/vendor/prismjs/components/prism-kotlin.js")

    // custom languages (or ones not yet present in webjars)
    resourceJs("/assets/ktorfx/prismjs/aql.js")
})

