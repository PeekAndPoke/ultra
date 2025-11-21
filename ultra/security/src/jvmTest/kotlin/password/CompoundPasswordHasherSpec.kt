package de.peekandpoke.ultra.security.password

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CompoundPasswordHasherSpec : FreeSpec() {

    class PlaintextPasswordHasher(
        override val id: String = "plaintext",
    ) : PasswordHasher {
        override fun hash(password: String): PasswordHasher.Hash {
            return PasswordHasher.Hash(
                id = id,
                salt = "",
                hash = password,
            )
        }

        override fun check(plaintext: String?, hash: PasswordHasher.Hash?): Boolean {
            return plaintext == hash?.hash
        }
    }

    init {
        "Instantiating" - {
            "must work with a single hasher" {
                val plaintext = PlaintextPasswordHasher()
                val subject = CompoundPasswordHasher(listOf(plaintext))

                subject.hashers.size shouldBe 1
                subject.primary shouldBe plaintext
            }

            "must work with multiple hashers" {
                val plaintext = PlaintextPasswordHasher()
                val pbkdf2 = PBKDF2WithHmacSHA256PasswordHasher.i65536k256
                val subject = CompoundPasswordHasher(plaintext, pbkdf2)

                subject.hashers.size shouldBe 2
                subject.primary shouldBe plaintext

                subject.hashers[plaintext.id] shouldBe plaintext
                subject.hashers[pbkdf2.id] shouldBe pbkdf2
            }
        }

        "Hashing a password" - {

            "must use the primary hasher" {
                val plaintext = PlaintextPasswordHasher()
                val pbkdf2 = PBKDF2WithHmacSHA256PasswordHasher.i65536k256
                val subject = CompoundPasswordHasher(pbkdf2, plaintext)

                subject.primary shouldBe pbkdf2

                val hash = subject.hash("super-secret")

                hash.id shouldBe pbkdf2.id

                withClue("must be checkable with the primary hasher") {
                    pbkdf2.check("super-secret", hash) shouldBe true
                }

                withClue("must not be checkable with the other hasher") {
                    plaintext.check("super-secret", hash) shouldBe false
                }
            }
        }

        "Checking a password" - {

            "must work when the hash was created by any of the hashers" {

                val plaintext = PlaintextPasswordHasher()
                val pbkdf2 = PBKDF2WithHmacSHA256PasswordHasher.i65536k256
                val subject = CompoundPasswordHasher(pbkdf2, plaintext)

                val hashByPbkdf2 = pbkdf2.hash("super-secret-1")
                val hashByPlaintext = plaintext.hash("super-secret-2")

                subject.check("super-secret-1", hashByPbkdf2) shouldBe true
                subject.check("super-secret-2", hashByPlaintext) shouldBe true
            }

            "must not work when the hash was not created by any of the hashers" {
                val plaintext = PlaintextPasswordHasher()
                val pbkdf2 = PBKDF2WithHmacSHA256PasswordHasher.i65536k256
                val subject = CompoundPasswordHasher(pbkdf2, plaintext)

                val hash = PasswordHasher.Hash(
                    id = "unknown",
                    salt = "",
                    hash = "a-hash"
                )

                subject.check("a-password", hash) shouldBe false
            }
        }
    }
}
