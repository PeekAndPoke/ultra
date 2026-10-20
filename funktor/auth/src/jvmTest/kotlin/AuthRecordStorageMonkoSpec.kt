package io.peekandpoke.funktor.auth

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.kotest.matchers.shouldBe
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import org.bson.Document
import org.bson.types.ObjectId

class AuthRecordStorageMonkoSpec : AuthRecordStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorAuthBuilder.configureAuth() {
        useMonko()
    }

    override suspend fun Kontainer.stripClaimedBy(key: String) {
        val bsonId: Any = if (ObjectId.isValid(key)) ObjectId(key) else key
        get(MonkoDriver::class).database.getCollection<Document>("system_auth_records")
            .updateOne(Filters.eq("_id", bsonId), Updates.unset("claimedBy"))
            .matchedCount shouldBe 1L
    }
}
