package io.peekandpoke.funktor.auth

import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.ultra.kontainer.KontainerBuilder

class AuthRecordStorageKarangoSpec : AuthRecordStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useKarango()
    }
}
