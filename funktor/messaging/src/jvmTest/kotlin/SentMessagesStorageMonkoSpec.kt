package io.peekandpoke.funktor.messaging

import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.KontainerBuilder

class SentMessagesStorageMonkoSpec : SentMessagesStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorMessagingBuilder.configureMessaging() {
        useMonko()
    }
}
