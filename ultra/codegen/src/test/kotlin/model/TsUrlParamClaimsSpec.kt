package io.peekandpoke.ultra.codegen.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TsUrlParamClaimsSpec : FreeSpec() {

    init {
        "a claim is found by its class, whatever the instantiation" {
            val claims = TsUrlParamClaims()

            claims.scopeFor("alpha").map<FxSpeaker>(tsType = "string", format = "the speaker id")

            claims.find(FxSpeaker::class)?.tsType shouldBe "string"
            claims.find(FxSpeaker::class)?.format shouldBe "the speaker id"
            claims.find(FxSpeaker::class)?.claimedBy shouldBe "alpha"
        }

        "an unclaimed type is not found" {
            TsUrlParamClaims().find(FxSpeaker::class) shouldBe null
        }

        "a second claim for the same type is rejected, naming both contributors" {
            // Never last-wins: contributors arrive from a DI container in no defined order, so a
            // silent overwrite would make the emitted signature depend on registration order.
            val claims = TsUrlParamClaims()

            claims.scopeFor("alpha").map<FxSpeaker>(tsType = "string")

            val thrown = runCatching {
                claims.scopeFor("beta").map<FxSpeaker>(tsType = "number")
            }.exceptionOrNull()

            thrown!!.message!! shouldContain "alpha"
            thrown.message!! shouldContain "beta"
            thrown.message!! shouldContain FxSpeaker::class.qualifiedName!!

            withClue("the first claim must survive, rather than being half-overwritten") {
                claims.find(FxSpeaker::class)?.tsType shouldBe "string"
            }
        }

        "the same contributor claiming a type twice is rejected too" {
            // Not a special case worth allowing: it is still ambiguous which one wins.
            val claims = TsUrlParamClaims()
            val scope = claims.scopeFor("alpha")

            scope.map<FxSpeaker>(tsType = "string")

            val thrown = runCatching { scope.map<FxSpeaker>(tsType = "number") }.exceptionOrNull()

            thrown?.message shouldContain "claimed twice"
        }

        "claims are listed in registration order" {
            val claims = TsUrlParamClaims()
            val scope = claims.scopeFor("alpha")

            scope.map<FxSpeaker>(tsType = "string")
            scope.map<FxTalk>(tsType = "number")

            claims.all().map { it.qualifiedName } shouldBe listOf(
                FxSpeaker::class.qualifiedName,
                FxTalk::class.qualifiedName,
            )
        }
    }
}
