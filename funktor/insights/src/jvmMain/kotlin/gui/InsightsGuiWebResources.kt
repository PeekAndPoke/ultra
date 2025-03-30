package de.peekandpoke.ktorfx.insights.gui

import de.peekandpoke.ktorfx.core.model.CacheBuster
import de.peekandpoke.ktorfx.staticweb.resources.WebResourceGroup
import de.peekandpoke.ktorfx.staticweb.resources.WebResources

val WebResources.insightsGui get() = get(InsightsGuiWebResources::class)

class InsightsGuiWebResources(buster: CacheBuster) : WebResourceGroup(buster, {

    resourceCss("/assets/ktorfx/insights/gui.css")

    resourceJs("/assets/ktorfx/insights/semanticui-tablesort.js")
    resourceJs("/assets/ktorfx/insights/gui.js")
})
