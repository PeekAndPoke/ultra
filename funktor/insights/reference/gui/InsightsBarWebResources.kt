package io.peekandpoke.funktor.insights.gui

import io.peekandpoke.funktor.core.model.CacheBuster
import io.peekandpoke.funktor.staticweb.resources.WebResourceGroup
import io.peekandpoke.funktor.staticweb.resources.WebResources

val WebResources.insightsBar get() = get(InsightsBarWebResources::class)

class InsightsBarWebResources(buster: CacheBuster) : WebResourceGroup(buster, {
    resourceCss("/assets/funktor/insights/bar.css")
    resourceJs("/assets/funktor/insights/bar.js")
})
