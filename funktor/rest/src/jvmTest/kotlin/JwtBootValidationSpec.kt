package io.peekandpoke.funktor.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.jwt.JwtConfig
import io.peekandpoke.ultra.security.jwt.JwtSigningKey

/**
 * `funktorRest { jwt(...) }` must reject a broken key set **while the kontainer blueprint is being
 * built** — i.e. at application boot.
 *
 * The `JwtGenerator` binding is a lazy singleton, so a check performed only inside its factory would
 * not run until the first request carrying an `Authorization: Bearer` header. On a live server that
 * is a 500 long after deploy, on a code path that is by definition reachable by an unauthenticated
 * caller. `JwtSignatureGate`'s own `require` covers every other construction path; this spec covers
 * the wiring that makes a misconfigured deployment refuse to start.
 *
 * Without it, deleting the `requireUsableKeys` call from `FunktorRestBuilder.jwt` fails nothing:
 * `JwtSignatureGateSpec` proves the check works, not that funktor invokes it.
 */
class JwtBootValidationSpec : StringSpec({

    val goodSecret = "boot-check-signing-key-rfc7518-needs-sixty-four-bytes-min!!!!!!!!"

    fun key(id: String, secret: String = goodSecret) = JwtSigningKey(id = id, secret = Redacted(secret))

    fun jwtConfig(keys: List<JwtSigningKey>) = JwtConfig(
        keys = keys,
        issuer = "boot-iss",
        audience = "boot-aud",
        permissionsNs = "permissions",
        userNs = "user",
    )

    /** Builds the blueprint only — never resolves the singleton, so a lazy check would not fire. */
    fun boot(keys: List<JwtSigningKey>) = kontainer {
        funktorRest(AppConfig.empty) { jwt(jwtConfig(keys)) }
    }

    "a usable key set boots" {
        boot(listOf(key("current"), key("previous")))
    }

    "an empty key list refuses to boot" {
        shouldThrow<IllegalArgumentException> { boot(emptyList()) }
            .message!! shouldContain "no signing keys"
    }

    "a secret under its algorithm's floor refuses to boot" {
        val thrown = shouldThrow<IllegalArgumentException> { boot(listOf(key("current", "too-short"))) }

        thrown.message!! shouldContain "RFC 7518"
        withClue("the message must name the offending key, and say how to fix it") {
            thrown.message!! shouldContain "'current'"
            thrown.message!! shouldContain "openssl rand -base64 64"
        }
    }

    "duplicate key ids refuse to boot" {
        // A `kid` names exactly one key. Duplicates would silently keep the last, so tokens signed
        // under the first key of that id would stop verifying — with nothing in the config to show why.
        shouldThrow<IllegalArgumentException> { boot(listOf(key("dup"), key("dup"))) }
            .message!! shouldContain "[dup]"
    }

    "a null JwtConfig refuses to boot" {
        shouldThrow<IllegalStateException> {
            kontainer { funktorRest(AppConfig.empty) { jwt(null) } }
        }
    }
})
