package io.peekandpoke.funktor.rest.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.ultra.security.user.UserPermissions

class CallerSpec : StringSpec({

    "Caller variants share the sealed parent so principal<Caller>() can fetch any" {
        val anon: Caller = Caller.AnonymousCaller
        anon.shouldBeInstanceOf<Caller>()

        val apiKey: Caller = Caller.ApiKeyCaller(keyId = "k1", userId = "u1")
        apiKey.shouldBeInstanceOf<Caller>()
    }

    "AnonymousCaller is a singleton" {
        Caller.AnonymousCaller shouldBeSameInstanceAs Caller.AnonymousCaller
    }

    "ApiKeyCaller carries the full key + identity payload" {
        val perms = UserPermissions(roles = setOf("ci"))
        val subject = Caller.ApiKeyCaller(
            keyId = "k1",
            keyName = "ci-deploy",
            userId = "alice",
            email = "alice@example.com",
            desc = "Alice",
            type = "user",
            permissions = perms,
        )

        subject.keyId shouldBe "k1"
        subject.keyName shouldBe "ci-deploy"
        subject.userId shouldBe "alice"
        subject.email shouldBe "alice@example.com"
        subject.desc shouldBe "Alice"
        subject.type shouldBe "user"
        subject.permissions shouldBe perms
    }

    "ApiKeyCaller has sensible defaults" {
        val subject = Caller.ApiKeyCaller(keyId = "k1", userId = "u1")

        subject.keyName shouldBe null
        subject.email shouldBe null
        subject.desc shouldBe null
        subject.type shouldBe null
        subject.permissions shouldBe UserPermissions.anonymous
    }

    "Caller variants are exhaustively pattern-matchable" {
        val callers: List<Caller> = listOf(
            Caller.AnonymousCaller,
            Caller.ApiKeyCaller(keyId = "k", userId = "u"),
        )

        val labels = callers.map { c ->
            when (c) {
                is Caller.AnonymousCaller -> "anon"
                is Caller.JwtCaller -> "jwt"
                is Caller.ApiKeyCaller -> "api-key"
            }
        }

        labels shouldBe listOf("anon", "api-key")
    }
})
