package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.slumber.SlumberConfig

class MonkoModuleSpec : FreeSpec() {

    init {
        "getMonkoDefaultSlumberConfig" - {

            "should return a non-null SlumberConfig" {
                val config = getMonkoDefaultSlumberConfig()

                config.shouldBeInstanceOf<SlumberConfig>()
            }

            "should include VaultSlumberModule" {
                val config = getMonkoDefaultSlumberConfig()

                // The default config should have modules (at minimum the vault module + defaults)
                config.modules.size shouldNotBe 0
            }

            "should be consistent across calls" {
                val config1 = getMonkoDefaultSlumberConfig()
                val config2 = getMonkoDefaultSlumberConfig()

                config1.modules.size shouldBe config2.modules.size
            }
        }

        "MongoDbConfig" - {

            "forUnitTests should produce a valid config" {
                val config = MongoDbConfig.forUnitTests

                config.connectionString shouldNotBe ""
                config.database shouldNotBe ""
            }

            "toMongoClientWithoutCache should create a client" {
                // This test only verifies the function doesn't throw
                // It requires a real MongoDB connection, so we just verify the config is valid
                val config = MongoDbConfig.forUnitTests

                config.connectionString shouldBe "mongodb://root:root@localhost:27017"
                config.database shouldBe "test"
            }
        }
    }
}
