package io.peekandpoke.funktor.demo.server.admin

import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.module

inline val KontainerAware.adminUsers: AdminUserServices get() = kontainer.get()
inline val ApplicationCall.adminUsers: AdminUserServices get() = kontainer.adminUsers
inline val RoutingContext.adminUsers: AdminUserServices get() = call.adminUsers

val AdminUserModule = module {

    dynamic(AdminUserRealm::class)

    dynamic(AdminUserServices::class)
//    singleton(AppUserApiFeature::class)

    dynamic(AdminUsersRepo::class)
    dynamic(AdminUsersRepo.Fixtures::class)
}
