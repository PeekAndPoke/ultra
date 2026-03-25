package io.peekandpoke.funktor.staticweb.templating

import io.peekandpoke.funktor.core.broker.TypedRouteRenderer
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.staticweb.flashsession.FlashSession
import io.peekandpoke.funktor.staticweb.resources.WebResources

interface TemplateTools {
    val config: AppConfig
    val flash: FlashSession
    val resource: WebResources
    val routes: TypedRouteRenderer
}
