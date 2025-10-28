package de.peekandpoke.monko

data class MongoDbConfig(
    val connectionString: String,
    val database: String,
) {
    companion object {
        val forUnitTests
            get() = MongoDbConfig(
                connectionString = "mongodb://root:root@localhost:27017",
                database = "test"
            )
    }
}
