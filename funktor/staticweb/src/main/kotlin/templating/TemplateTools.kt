package de.peekandpoke.ktorfx.staticweb.templating

import de.peekandpoke.ktorfx.core.broker.TypedRouteRenderer
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.staticweb.flashsession.FlashSession
import de.peekandpoke.ktorfx.staticweb.resources.WebResources

interface TemplateTools {
    val config: AppConfig
    val flash: FlashSession
    val resource: WebResources
    val routes: TypedRouteRenderer
}
