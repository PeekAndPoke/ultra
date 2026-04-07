package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class VaultConfigSpec : StringSpec({

    "VaultConfig default has profile=false and explain=false" {
        val config = VaultConfig()

        config.profile shouldBe false
        config.explain shouldBe false
    }

    "VaultConfig.default companion returns same defaults" {
        val config = VaultConfig.default

        config.profile shouldBe false
        config.explain shouldBe false
    }

    "VaultConfig copy changes only specified fields" {
        val original = VaultConfig(profile = true, explain = true)
        val copied = original.copy(explain = false)

        copied.profile shouldBe true
        copied.explain shouldBe false
    }
})
