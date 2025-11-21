package de.peekandpoke.funktor.auth

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.be
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

class AuthRandomSpec : FreeSpec() {

    init {

        "getToken" - {

            "should return a token with the correct length" {
                val lengths = listOf(1, 2, 16, 32, 128, 256, 512)

                lengths.forEach { length ->
                    val result = AuthRandom.default.getToken(length)
                    result.size should be(length)
                }
            }

            "should return different tokens when called multiple times" {
                val result1 = AuthRandom.default.getToken()
                val result2 = AuthRandom.default.getToken()

                result1 shouldNotBe result2
            }
        }

        "getTokenAsBase64" - {

            "should return a token that can be decoded from base64" {
                val lengths = listOf(1, 2, 16, 32, 128, 256, 512)

                lengths.forEach { length ->
                    val result = AuthRandom.default.getTokenAsBase64(length)
                    val decoded = Base64.getDecoder().decode(result)

                    decoded.size shouldBe length
                }
            }

            "should return different tokens when called multiple times" {
                val result1 = AuthRandom.default.getTokenAsBase64()
                val result2 = AuthRandom.default.getTokenAsBase64()

                result1 shouldNotBe result2
            }
        }
    }
}
