package de.peekandpoke.funktor.insights.gui

import de.peekandpoke.funktor.core.model.CacheBuster
import de.peekandpoke.funktor.staticweb.resources.WebResourceGroup
import de.peekandpoke.funktor.staticweb.resources.WebResources

val WebResources.insightsGui get() = get(InsightsGuiWebResources::class)

class InsightsGuiWebResources(buster: CacheBuster) : WebResourceGroup(buster, {

    resourceCss("/assets/funktor/insights/gui.css")

    resourceJs("/assets/funktor/insights/semanticui-tablesort.js")
    resourceJs("/assets/funktor/insights/gui.js")
})
