package de.peekandpoke.funktor.auth

import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.karango.karango
import de.peekandpoke.ultra.kontainer.KontainerBuilder

class AuthRecordStorageKarangoSpec : AuthRecordStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useKarango()
    }
}
