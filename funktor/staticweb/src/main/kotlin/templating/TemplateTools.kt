package de.peekandpoke.funktor.staticweb.templating

import de.peekandpoke.funktor.core.broker.TypedRouteRenderer
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.staticweb.flashsession.FlashSession
import de.peekandpoke.funktor.staticweb.resources.WebResources

interface TemplateTools {
    val config: AppConfig
    val flash: FlashSession
    val resource: WebResources
    val routes: TypedRouteRenderer
}
