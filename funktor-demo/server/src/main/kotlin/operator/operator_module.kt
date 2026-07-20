package io.peekandpoke.funktor.demo.server.operator

import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.module

inline val KontainerAware.operators: OperatorServices get() = kontainer.get()
inline val ApplicationCall.operators: OperatorServices get() = kontainer.operators
inline val RoutingContext.operators: OperatorServices get() = call.operators

val OperatorModule = module {

    dynamic(OperatorRealm::class)

    dynamic(OperatorServices::class)

    dynamic(OperatorUsersRepo::class)
    dynamic(OperatorUsersRepo.Fixtures::class)

    // Operator console API (collected by ApiApp as an ApiFeature)
    singleton(OperatorApiFeature::class)
}
