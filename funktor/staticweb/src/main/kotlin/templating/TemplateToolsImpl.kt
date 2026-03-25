package io.peekandpoke.funktor.staticweb.templating

import io.peekandpoke.funktor.core.broker.TypedRouteRenderer
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.staticweb.flashsession.FlashSession
import io.peekandpoke.funktor.staticweb.resources.WebResources

class TemplateToolsImpl(
    override val config: AppConfig,
    override val flash: FlashSession,
    override val resource: WebResources,
    override val routes: TypedRouteRenderer,
) : TemplateTools
