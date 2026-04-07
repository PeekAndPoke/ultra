package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class VaultExceptionSpec : StringSpec({

    "VaultException with message only" {
        val ex = VaultException("something went wrong")

        ex.message shouldBe "something went wrong"
        ex.cause shouldBe null
    }

    "VaultException with message and cause" {
        val cause = RuntimeException("root cause")
        val ex = VaultException("wrapper", cause)

        ex.message shouldBe "wrapper"
        ex.cause shouldBe cause
    }

    "VaultException is a Throwable" {
        val ex = VaultException("test")

        ex.shouldBeInstanceOf<Throwable>()
    }
})
