package de.peekandpoke.funktor.staticweb.resources

import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.staticweb.resources.common.funktorCommonWebResources
import de.peekandpoke.funktor.staticweb.resources.prismjs.funktorPrismJs
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*

internal fun KontainerBuilder.funktorResources() = module(Funktor_Resources)

inline val KontainerAware.webResources: WebResources get() = kontainer.get(WebResources::class)
inline val ApplicationCall.webResources: WebResources get() = kontainer.webResources
inline val RoutingContext.webResources: WebResources get() = call.webResources

internal val Funktor_Resources = module {
    // Web resources service
    singleton(WebResources::class)

    // modules
    funktorCommonWebResources()
    funktorPrismJs()
}
