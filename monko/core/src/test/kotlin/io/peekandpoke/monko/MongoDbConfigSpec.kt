package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MongoDbConfigSpec : FreeSpec() {

    init {
        "MongoDbConfig" - {

            "forUnitTests should have correct connection string" {
                val config = MongoDbConfig.forUnitTests

                config.connectionString shouldBe "mongodb://root:root@localhost:27017"
            }

            "forUnitTests should use 'test' database" {
                val config = MongoDbConfig.forUnitTests

                config.database shouldBe "test"
            }

            "should support custom configuration" {
                val config = MongoDbConfig(
                    connectionString = "mongodb://user:pass@host:12345",
                    database = "mydb",
                )

                config.connectionString shouldBe "mongodb://user:pass@host:12345"
                config.database shouldBe "mydb"
            }

            "should be a data class with correct equality" {
                val config1 = MongoDbConfig(
                    connectionString = "mongodb://localhost:27017",
                    database = "test",
                )
                val config2 = MongoDbConfig(
                    connectionString = "mongodb://localhost:27017",
                    database = "test",
                )

                config1 shouldBe config2
            }

            "different configs should not be equal" {
                val config1 = MongoDbConfig(
                    connectionString = "mongodb://localhost:27017",
                    database = "test1",
                )
                val config2 = MongoDbConfig(
                    connectionString = "mongodb://localhost:27017",
                    database = "test2",
                )

                (config1 == config2) shouldBe false
            }

            "should support copy" {
                val original = MongoDbConfig.forUnitTests
                val copied = original.copy(database = "other_db")

                copied.connectionString shouldBe original.connectionString
                copied.database shouldBe "other_db"
            }
        }
    }
}
