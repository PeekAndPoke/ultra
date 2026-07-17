package io.peekandpoke.funktor.saas

import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.ultra.kontainer.KontainerBuilder

class OrgsStorageKarangoSpec : OrgsStorageBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorSaasBuilder.configureSaas() {
        useKarango()
    }
}
