package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.ultra.kontainer.module

val B2bModule = module {

    dynamic(B2bRealm::class)

    dynamic(B2bUsersRepo::class)
    dynamic(B2bUsersRepo.Fixtures::class)

    singleton(B2bMembersApiFeature::class)
    dynamic(B2bMembersServices::class)
}
