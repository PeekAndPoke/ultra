package de.peekandpoke.funktor.staticweb.resources.prismjs

import de.peekandpoke.funktor.core.model.CacheBuster
import de.peekandpoke.funktor.staticweb.resources.WebResourceGroup
import de.peekandpoke.funktor.staticweb.resources.WebResources
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

/**
 * Imports the [Funktor_PrismJs] into the [KontainerBuilder]
 */
fun KontainerBuilder.funktorPrismJs() = module(Funktor_PrismJs)

val WebResources.prismJs get() = get(PrismJsWebResources::class)

/**
 * Defines the prism js module
 */
val Funktor_PrismJs = module {
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
    resourceJs("/assets/funktor/prismjs/aql.js")
})

