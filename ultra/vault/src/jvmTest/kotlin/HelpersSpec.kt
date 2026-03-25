package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HelpersSpec : StringSpec({

    "ensureKey extracts key from id with slash" {
        "collection/abc123".ensureKey shouldBe "abc123"
    }

    "ensureKey returns input when no slash" {
        "abc123".ensureKey shouldBe "abc123"
    }

    "ensureKey with empty collection" {
        "/key".ensureKey shouldBe "key"
    }
})
