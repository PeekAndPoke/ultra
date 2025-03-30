package de.peekandpoke.ktorfx.insights.gui

import de.peekandpoke.ktorfx.core.model.CacheBuster
import de.peekandpoke.ktorfx.staticweb.resources.WebResourceGroup
import de.peekandpoke.ktorfx.staticweb.resources.WebResources

val WebResources.insightsBar get() = get(InsightsBarWebResources::class)

class InsightsBarWebResources(buster: CacheBuster) : WebResourceGroup(buster, {
    resourceCss("/assets/ktorfx/insights/bar.css")
    resourceJs("/assets/ktorfx/insights/bar.js")
})
