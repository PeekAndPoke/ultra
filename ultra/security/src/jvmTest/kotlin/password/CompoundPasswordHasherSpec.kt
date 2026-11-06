package io.peekandpoke.ultra.security.password

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

            "must answer false, not throw, for a stored hash its hasher cannot decode" {
                val argon2 = Argon2PasswordHasher.id_m65536i2p4o32
                val bcrypt = BcryptPasswordHasher.l12y
                val subject = CompoundPasswordHasher(argon2, bcrypt)

                listOf(argon2, bcrypt).forEach { leaf ->
                    listOf(
                        "", "garbage", "\$argon2id\$v=19\$m=1,t=1,p=1\$!!\$!!", "\$argon2id\$v=19\$m=1\$AAAA\$AAAA",
                        "\$2y\$12\$short",
                    ).forEach { bad ->
                        withClue("${leaf.id} <- '$bad'") {
                            val hash = PasswordHasher.Hash(id = leaf.id, salt = "", hash = bad)
                            subject.check("a-password", hash) shouldBe false
                        }
                    }
                }
            }
        }

        "Rehash (ULTRA-00066)" - {

            "a hash from the primary needs none; one from another known hasher, an unknown one or none does" {
                val argon2 = Argon2PasswordHasher.id_m65536i2p4o32
                val pbkdf2 = PBKDF2WithHmacSHA256PasswordHasher.i65536k256
                val subject = CompoundPasswordHasher(argon2, pbkdf2)

                subject.needsRehash(subject.hash("pw")) shouldBe false
                subject.needsRehash(subject.hash("pw").asString()) shouldBe false
                subject.needsRehash(pbkdf2.hash("pw")) shouldBe true
                subject.needsRehash(pbkdf2.hash("pw").asString()) shouldBe true
                subject.needsRehash(PasswordHasher.Hash(id = "unknown", salt = "", hash = "x")) shouldBe true
                subject.needsRehash(null as PasswordHasher.Hash?) shouldBe true
            }

            "a leaf hasher needs a rehash for any other id" {
                val pbkdf2 = PBKDF2WithHmacSHA256PasswordHasher.i65536k256

                pbkdf2.needsRehash(pbkdf2.hash("pw")) shouldBe false
                pbkdf2.needsRehash(PBKDF2WithHmacSHA256PasswordHasher.i310000k256.hash("pw")) shouldBe true
            }
        }
    }
}
