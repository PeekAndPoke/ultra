package io.peekandpoke.funktor.demo.server.funktorconf

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.demo.server.api.funktorconf.FunktorConfApiFeature
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.module

inline val KontainerAware.funktorConf: FunktorConfServices get() = kontainer.get()
inline val ApplicationCall.funktorConf: FunktorConfServices get() = kontainer.funktorConf
inline val RoutingContext.funktorConf: FunktorConfServices get() = call.funktorConf

val FunktorConfModule = module {
    singleton(FunktorConfApiFeature::class)

    dynamic(FunktorConfServices::class)

    dynamic(EventsRepo::class)
    dynamic(EventsRepo.Fixtures::class)

    dynamic(SpeakersRepo::class)
    dynamic(SpeakersRepo.Fixtures::class)

    dynamic(AttendeesRepo::class)
    dynamic(AttendeesRepo.Fixtures::class)
}
