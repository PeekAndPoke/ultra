package de.peekandpoke.funktor.staticweb.resources.common

import de.peekandpoke.funktor.core.model.CacheBuster
import de.peekandpoke.funktor.staticweb.resources.WebResourceGroup
import de.peekandpoke.funktor.staticweb.resources.WebResources
import de.peekandpoke.funktor.staticweb.resources.prismjs.funktorPrismJs
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.funktorCommonWebResources() = module(Funktor_CommonWebResources)

val WebResources.fomanticUi get() = get(FomanticUiWebResources::class)

internal val Funktor_CommonWebResources = module {
    // Common web resources
    singleton(FomanticUiWebResources::class)
    singleton(JQueryWebResource::class)
    singleton(LazySizesWebResource::class)
    singleton(VisJsWebResource::class)

    funktorPrismJs()
}

class FomanticUiWebResources(cacheBuster: CacheBuster) : WebResourceGroup(cacheBuster, {
    webjarCss("/vendor/fomantic-ui-css/semantic.min.css")
    webjarJs("/vendor/fomantic-ui-css/semantic.min.js")
})

class JQueryWebResource(cacheBuster: CacheBuster) : WebResourceGroup(cacheBuster, {
    webjarJs("/vendor/jquery/jquery.min.js")
})

val WebResources.jQuery get() = get(JQueryWebResource::class)

class LazySizesWebResource(cacheBuster: CacheBuster) : WebResourceGroup(cacheBuster, {
    webjarJs("/vendor/lazysizes/lazysizes.min.js")
})

val WebResources.lazySizes get() = get(LazySizesWebResource::class)

class VisJsWebResource(cacheBuster: CacheBuster) : WebResourceGroup(cacheBuster, {
    webjarJs("/vendor/visjs/vis.min.js")
})

val WebResources.vizJs get() = get(VisJsWebResource::class)
