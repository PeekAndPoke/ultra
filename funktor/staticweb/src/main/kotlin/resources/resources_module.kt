package de.peekandpoke.ktorfx.staticweb.resources

import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ktorfx.staticweb.resources.common.ktorFxCommonWebResources
import de.peekandpoke.ktorfx.staticweb.resources.prismjs.ktorFxPrismJs
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*

internal fun KontainerBuilder.ktorFxResources() = module(KtorFX_Resources)

inline val KontainerAware.webResources: WebResources get() = kontainer.get(WebResources::class)
inline val ApplicationCall.webResources: WebResources get() = kontainer.webResources
inline val RoutingContext.webResources: WebResources get() = call.webResources

internal val KtorFX_Resources = module {
    // Web resources service
    singleton(WebResources::class)

    // modules
    ktorFxCommonWebResources()
    ktorFxPrismJs()
}
