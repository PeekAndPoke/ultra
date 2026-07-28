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

    // Document keys are not supposed to contain slashes — ArangoDB forbids it outright, and the
    // `collection/key` id format only parses unambiguously without them. These pin what happens to
    // a malformed id anyway: everything after the FIRST slash, never a middle segment.

    "ensureKey splits at the first slash, not the second" {
        "collection/a/b".ensureKey shouldBe "a/b"
    }

    "ensureKey of an id without a key is empty" {
        "collection/".ensureKey shouldBe ""
    }
})
