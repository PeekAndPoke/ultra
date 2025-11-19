package de.peekandpoke.funktor.auth

import de.peekandpoke.monko.MongoDbConfig
import de.peekandpoke.monko.monko
import de.peekandpoke.ultra.kontainer.KontainerBuilder

class AuthRecordStorageMonkoSpec : AuthRecordStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useMonko()
    }
}
