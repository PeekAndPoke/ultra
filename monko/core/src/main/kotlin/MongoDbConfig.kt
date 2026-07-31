package io.peekandpoke.monko

import io.peekandpoke.ultra.common.model.Redacted

data class MongoDbConfig(
    /** Carries user:password@host in the ordinary case, so the whole URI is a credential. */
    val connectionString: Redacted<String>,
    val database: String,
) {
    companion object {
        val forUnitTests
            get() = MongoDbConfig(
                connectionString = Redacted("mongodb://root:root@localhost:27017"),
                database = "test"
            )
    }
}
