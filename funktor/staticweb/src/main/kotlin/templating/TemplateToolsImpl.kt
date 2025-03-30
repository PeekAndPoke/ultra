package de.peekandpoke.ktorfx.staticweb.templating

import de.peekandpoke.ktorfx.core.broker.TypedRouteRenderer
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.staticweb.flashsession.FlashSession
import de.peekandpoke.ktorfx.staticweb.resources.WebResources

class TemplateToolsImpl(
    override val config: AppConfig,
    override val flash: FlashSession,
    override val resource: WebResources,
    override val routes: TypedRouteRenderer,
) : TemplateTools
