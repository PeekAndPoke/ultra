package io.peekandpoke.funktor.staticweb.resources

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.staticweb.resources.common.funktorCommonWebResources
import io.peekandpoke.funktor.staticweb.resources.prismjs.funktorPrismJs
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

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
