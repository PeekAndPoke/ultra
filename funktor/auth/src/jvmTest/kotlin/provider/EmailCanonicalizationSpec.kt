package io.peekandpoke.funktor.auth.provider

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.MinimalTestDeps
import io.peekandpoke.funktor.auth.MinimalTestRealm
import io.peekandpoke.funktor.auth.MinimalTestUser
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.vault.Stored
import java.util.concurrent.atomic.AtomicReference

/**
 * Pins that email lookups are CASE-INSENSITIVE BY TYPE.
 *
 * Emails were always stored canonically, but the lookups (`loadByEmail`, the repos' `findByEmail`)
 * are case-sensitive exact matches and used to trust each caller to normalize. Several did not, and
 * every consequence was SILENT:
 *
 * | call site | old behaviour with a mixed-case address |
 * |---|---|
 * | password-reset init | no user found → **no reset email sent**, and the endpoint still reports success |
 * | Google / GitHub SSO login | no match → login fails for an existing account |
 * | Google / GitHub SSO signup | existence check misses → **duplicate user** / unique-index error |
 *
 * `EmailAddress` makes that unrepresentable: `loadByEmail` takes one, and it can only be built
 * canonically. These tests assert the CANONICAL value actually arrives at the lookup, so a future
 * refactor that reintroduces a raw-string path fails here.
 */
class EmailCanonicalizationSpec : FreeSpec({

    val urls = EmailAndPasswordAuth.FrontendUrls(baseUrl = "https://a.b.c")

    fun subject(vararg capabilities: AuthProviderModel.Capability) =
        EmailAndPasswordAuth.Factory(lazy { MinimalTestDeps() }, NullLog)(
            urls, capabilities.toSet().ifEmpty { setOf(AuthProviderModel.Capability.SignIn) },
        )

    "sign-in canonicalizes a mixed-case, padded address before the lookup" {
        val seen = AtomicReference<EmailAddress?>(null)

        val realm = MinimalTestRealm(
            onLoadUserByEmail = { email ->
                seen.set(email)
                null // not found — we only care what the lookup was ASKED for
            }
        )

        runCatching {
            subject().signIn(
                realm,
                AuthSignInRequest.EmailAndPassword(
                    provider = EmailAndPasswordAuth.ID,
                    email = "  User@EXAMPLE.com  ",
                    password = "password",
                ),
            )
        }

        seen.get() shouldBe EmailAddress("user@example.com")
    }

    "password-reset init canonicalizes before the lookup — the gap that silently sent no email" {
        val seen = AtomicReference<EmailAddress?>(null)
        val resolved = AtomicReference<Boolean?>(null)

        val user = Stored(_id = "users/u1", value = MinimalTestUser(email = EmailAddress("user@example.com")))

        val realm = MinimalTestRealm(
            onLoadUserByEmail = { email ->
                seen.set(email)
                // Only the CANONICAL address matches the stored user — exactly like the real
                // case-sensitive `findByEmail`.
                user.takeIf { email == EmailAddress("user@example.com") }
                    .also { resolved.set(it != null) }
            },
        )

        // The flow continues into token storage, which this minimal harness does not provide — we
        // only care that the lookup happened and what it resolved.
        runCatching {
            subject().recoverAccountInitPasswordReset(
                realm,
                AuthRecoverAccountRequest.InitPasswordReset(
                    provider = EmailAndPasswordAuth.ID,
                    email = "  User@EXAMPLE.com  ",
                ),
            )
        }

        seen.get() shouldBe EmailAddress("user@example.com")
        // THE regression guard: before this change the raw mixed-case address was passed straight to
        // the case-sensitive lookup, resolved nobody, and the endpoint still returned success — so
        // the user simply never received a reset mail.
        resolved.get() shouldBe true
    }

    "password-reset init returns the neutral response for a MALFORMED address (no enumeration)" {
        // A malformed-but-canonicalizable address is now LOOKED UP like any other — deliberately.
        // Rejecting it before the lookup would protect nothing (it simply does not match) while making
        // any account stored with such an address unreachable. It finds nobody and returns the same
        // neutral response as an unknown address.
        val seen = AtomicReference<EmailAddress?>(null)

        val realm = MinimalTestRealm(
            onLoadUserByEmail = { email ->
                seen.set(email)
                null
            }
        )

        val result = subject().recoverAccountInitPasswordReset(
            realm,
            AuthRecoverAccountRequest.InitPasswordReset(
                provider = EmailAndPasswordAuth.ID,
                email = "  Not-An-Email  ",
            ),
        )

        seen.get() shouldBe EmailAddress("not-an-email")
        result shouldBe AuthRecoverAccountResponse.InitPasswordReset
    }

    "password-reset init returns the neutral response for an UNCANONICALIZABLE address too" {
        // U+212A KELVIN SIGN lowercases to ASCII 'k', so it cannot be canonicalized safely, and
        // `parseOrNull` yields null and the lookup is skipped entirely — same neutral response, so the
        // three cases remain indistinguishable to a caller.
        val realm = MinimalTestRealm() // loadByEmail would throw if it were reached

        val result = subject().recoverAccountInitPasswordReset(
            realm,
            AuthRecoverAccountRequest.InitPasswordReset(
                provider = EmailAndPasswordAuth.ID,
                email = "\u212Aarsten@example.com",
            ),
        )

        result shouldBe AuthRecoverAccountResponse.InitPasswordReset
    }
})
