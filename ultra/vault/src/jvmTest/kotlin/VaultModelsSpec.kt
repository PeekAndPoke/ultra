package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class VaultModelsSpec : StringSpec({

    // RepositoryInfo.hasErrors ////////////////////////////////////////////////////////////////////

    "RepositoryInfo.hasErrors is false when no missing or excess indexes" {
        val info = VaultModels.RepositoryInfo(
            connection = "conn",
            name = "test",
            stats = VaultModels.RepositoryStats.empty,
            indexes = VaultModels.IndexesInfo.empty,
        )

        info.hasErrors shouldBe false
    }

    "RepositoryInfo.hasErrors is true when there are missing indexes" {
        val info = VaultModels.RepositoryInfo(
            connection = "conn",
            name = "test",
            stats = VaultModels.RepositoryStats.empty,
            indexes = VaultModels.IndexesInfo(
                healthyIndexes = emptyList(),
                missingIndexes = listOf(
                    VaultModels.IndexInfo(name = "idx", type = "hash", fields = listOf("field1"))
                ),
                excessIndexes = emptyList(),
            ),
        )

        info.hasErrors shouldBe true
    }

    "RepositoryInfo.hasErrors is true when there are excess indexes" {
        val info = VaultModels.RepositoryInfo(
            connection = "conn",
            name = "test",
            stats = VaultModels.RepositoryStats.empty,
            indexes = VaultModels.IndexesInfo(
                healthyIndexes = emptyList(),
                missingIndexes = emptyList(),
                excessIndexes = listOf(
                    VaultModels.IndexInfo(name = "idx", type = "hash", fields = listOf("field1"))
                ),
            ),
        )

        info.hasErrors shouldBe true
    }

    // RepositoryStats.empty //////////////////////////////////////////////////////////////////////

    "RepositoryStats.empty has null fields and empty lists" {
        val stats = VaultModels.RepositoryStats.empty

        stats.type shouldBe null
        stats.isSystem shouldBe null
        stats.status shouldBe null
        stats.custom.shouldBeEmpty()
    }

    // RepositoryStats.Custom.of //////////////////////////////////////////////////////////////////

    "RepositoryStats.Custom.of converts values to strings" {
        val custom = VaultModels.RepositoryStats.Custom.of(
            name = "metrics",
            entries = mapOf("count" to 42, "label" to "test", "nullable" to null),
        )

        custom.name shouldBe "metrics"
        custom.entries["count"] shouldBe "42"
        custom.entries["label"] shouldBe "test"
        custom.entries["nullable"] shouldBe null
    }
})
