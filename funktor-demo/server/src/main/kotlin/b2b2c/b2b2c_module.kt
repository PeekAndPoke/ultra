package io.peekandpoke.funktor.demo.server.b2b2c

import io.peekandpoke.ultra.kontainer.module

val B2b2cModule = module {

    dynamic(B2b2cRealm::class)

    dynamic(B2b2cUsersRepo::class)
    dynamic(B2b2cUsersRepo.Fixtures::class)
}
