package io.peekandpoke.funktor.messaging

import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.ultra.kontainer.KontainerBuilder

class SentMessagesStorageKarangoSpec : SentMessagesStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorMessagingBuilder.configureMessaging() {
        useKarango()
    }
}
