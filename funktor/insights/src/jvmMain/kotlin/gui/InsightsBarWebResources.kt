package de.peekandpoke.funktor.insights.gui

import de.peekandpoke.funktor.core.model.CacheBuster
import de.peekandpoke.funktor.staticweb.resources.WebResourceGroup
import de.peekandpoke.funktor.staticweb.resources.WebResources

val WebResources.insightsBar get() = get(InsightsBarWebResources::class)

class InsightsBarWebResources(buster: CacheBuster) : WebResourceGroup(buster, {
    resourceCss("/assets/funktor/insights/bar.css")
    resourceJs("/assets/funktor/insights/bar.js")
})
