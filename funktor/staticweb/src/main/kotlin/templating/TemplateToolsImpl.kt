package de.peekandpoke.funktor.staticweb.templating

import de.peekandpoke.funktor.core.broker.TypedRouteRenderer
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.staticweb.flashsession.FlashSession
import de.peekandpoke.funktor.staticweb.resources.WebResources

class TemplateToolsImpl(
    override val config: AppConfig,
    override val flash: FlashSession,
    override val resource: WebResources,
    override val routes: TypedRouteRenderer,
) : TemplateTools
