package io.peekandpoke.funktor.saas

import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.KontainerBuilder

class OrgMembersStorageMonkoSpec : OrgMembersStorageBaseSpec() {
    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorSaasBuilder.configureSaas() {
        useMonko()
    }
}
