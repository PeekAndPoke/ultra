package io.peekandpoke.funktor.auth

import com.arangodb.model.DocumentUpdateOptions
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import kotlinx.coroutines.future.await

class AuthRecordStorageKarangoSpec : AuthRecordStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useKarango()
    }

    override suspend fun Kontainer.stripClaimedBy(key: String) {
        // keepNull = false: a null value REMOVES the attribute instead of storing null.
        get(KarangoDriver::class).arangoDb.collection("system_auth_records")
            .updateDocument(key, mapOf("claimedBy" to null), DocumentUpdateOptions().keepNull(false))
            .await().key shouldBe key
    }
}
