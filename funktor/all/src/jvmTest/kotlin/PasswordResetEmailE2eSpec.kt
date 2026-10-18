package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.*
import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.senders.hrefs
import io.peekandpoke.ultra.common.decodeUriComponent
import io.peekandpoke.ultra.common.encodeUriComponent

/**
 * Walks the password-reset flow the way a user does: ask for a reset, READ THE EMAIL, follow the link
 * in it, set a new password, and sign in with it.
 *
 * The point is the middle step. Every part of this flow was already unit-tested, and the flow was
 * still only assumed to work, because nothing ever asserted that an email is sent at all — let alone
 * that the token inside it is the one the server will accept. Those are exactly the joints that break
 * silently: a reset that mails nothing, or mails a link whose token does not survive URL encoding,
 * looks perfectly healthy from every unit test and is completely broken for the user.
 *
 * Reading the mail from the SENDER, not from `SentMessagesStorage`, is deliberate: auth mails are
 * persisted with `EmailStoring.withAnonymizedContent`, which strips every href before storing.
 *
 * SCOPE: [TestUserRealm] is org-less. The mail half generalizes — every demo realm uses
 * `AuthRealm.DefaultMessaging` and `EmailAndPasswordAuth`, and `AuthRecord.PasswordRecoveryToken` has
 * no org dimension — but the closing sign-in leg does NOT. An `OrgPolicy.Required` realm answers
 * `issueSignIn` with `OrgSelectionRequired`, so "reset then sign in" stays unproven for org realms.
 */
class PasswordResetEmailE2eSpec : FunktorApiSpec() {

    private val api by service(AuthApiFeature::class)
    private val emails get() = capturedEmails

    private val realmParam = RealmParam(realm = TestUserRealm.REALM)
    private val provider = EmailAndPasswordAuth.ID

    // Unique per run: the test users repo is not a fixture, so it is not cleared between runs.
    private val email = "reset-${System.currentTimeMillis()}@test.com"
    private val unknownEmail = "no-such-user-${System.currentTimeMillis()}@test.com"
    private val originalPassword = "Original1234!"
    private val newPassword = "Rotated5678!"

    /** The link the realm builds, up to but excluding the token — see [AuthFrontendRoutes.resetPassword]. */
    private val resetLinkPrefix = "https://example.com/auth/$provider/reset-password/"

    init {
        // The account is created HERE, not inside the first test. Creating it in a test made the
        // later ones silently depend on it: running one alone failed with "expected not null", which
        // reads as "case-insensitive reset is broken" rather than "the account was never created".
        beforeSpec {
            apiApp {
                anonymous {
                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = originalPassword,
                            displayName = "Reset Me",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthSignUpResponse>().shouldNotBeNull()
                    }
                }
            }

            // The tripwire this replaces asserted that signup mailed NOTHING, and was written to go
            // red exactly when activation landed. It did. Signup now mails an activation link and the
            // account cannot sign in until it is followed — which this spec deliberately does NOT do,
            // because the reset flow below is what activates it (see the closing sign-in).
            withClue("signup must mail an activation link") {
                emails.lastTo(email).shouldNotBeNull()
                    .subject shouldBe "Funktor All Test: Activate your Account"
            }
        }

        "The password reset flow must be walkable end to end, through the email" {
            apiApp {
                anonymous {
                    emails.clear()

                    //  When the user asks for a reset  //////////////////////////////////////////

                    api.authLogin.recoverAccountInitPasswordReset(
                        realmParam,
                        body = AuthRecoverAccountRequest.InitPasswordReset(
                            provider = provider,
                            email = email,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    //  Then a mail actually goes out ...  ///////////////////////////////////////

                    val recoveryMail = emails.lastTo(email).shouldNotBeNull()

                    recoveryMail.subject shouldBe "Funktor All Test: Recover your Account"
                    recoveryMail.source shouldBe "test@example.com"

                    //  ... carrying a link the server will accept  //////////////////////////////

                    // `singleOrNull` is a real assertion, not laziness: a recovery mail should carry
                    // ONE link and nothing else — no support link to be confused with the real one,
                    // no tracking pixel. If a second link is ever added deliberately, change this
                    // deliberately too.
                    val link = recoveryMail.hrefs().singleOrNull().shouldNotBeNull()

                    link shouldStartWith resetLinkPrefix

                    val tokenSegment = link.removePrefix(resetLinkPrefix)

                    // The token is a random base64 string, so '+', '/' and '=' are all routine in it,
                    // and the frontend route is `/auth/{provider}/reset-password/{token}` — ONE path
                    // segment. Dropping the token in unencoded therefore splits it across several
                    // segments at every '/', the route stops matching, and the link is dead while
                    // every server-side test still passes. Both assertions below exist because
                    // decoding alone does not notice: undoing an encoding that never happened is a
                    // no-op on base64, so the token still round-trips and the flow still "works".
                    tokenSegment shouldNotContain "/"
                    tokenSegment shouldBe tokenSegment.decodeUriComponent().encodeUriComponent()

                    // Decoding here is what a browser does before handing the value to the route.
                    val token = tokenSegment.decodeUriComponent()

                    //  ... and the PERSISTED copy must not carry it  ////////////////////////////

                    // The reason `EmailStoring.withAnonymizedContent` exists: the sent-messages
                    // inspector shows stored mail to support staff, and a live reset token in there
                    // is an account-takeover handed over on a plate. Only reachable as an assertion
                    // now that the test app wires the storing hook — and it compares against the
                    // token the captured mail actually contains, so it cannot pass vacuously.
                    // Selected by TAG, not by position: this address now also has an activation mail
                    // (and a second recovery mail from a later test) under the same refs, so `last()`
                    // would make the assertion depend on storage order and could silently end up
                    // checking a different mail entirely.
                    val stored = sentMessages.findByRefs(setOf(email)).toList().map { it.value() }
                        .filter { "password-reset" in it.tags }

                    withClue("the reset mail must be persisted, otherwise this asserts nothing") {
                        stored.shouldNotBeEmpty()
                    }

                    val storedBody = (stored.last().content as SentMessageModel.Content.EmailContent)
                        .body.content

                    storedBody shouldNotContain token
                    storedBody shouldNotContain tokenSegment

                    // Positive assertions, because the two above cannot fail on an EMPTY body: switch
                    // the realm to `withoutContent` and the stored body becomes "n/a", leaving this
                    // reading as though it still proved anonymization.
                    storedBody shouldContain "#anonymized"
                    storedBody shouldContain "Click the link below to recover your account"

                    api.authLogin.recoverAccountValidatePasswordResetToken(
                        realmParam,
                        body = AuthRecoverAccountRequest.ValidatePasswordResetToken(
                            provider = provider,
                            token = token,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthRecoverAccountResponse.ValidatePasswordResetToken>()
                            ?.success shouldBe true
                    }

                    emails.clear()

                    //  And the token sets a new password  ///////////////////////////////////////

                    api.authLogin.recoverAccountSetPasswordWithToken(
                        realmParam,
                        body = AuthRecoverAccountRequest.SetPasswordWithToken(
                            provider = provider,
                            token = token,
                            password = newPassword,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthRecoverAccountResponse.SetPasswordWithToken>()
                            ?.success shouldBe true
                    }

                    emails.lastTo(email).shouldNotBeNull()
                        .subject shouldBe "Funktor All Test: Your password was changed"

                    //  ... which is the one that now works  /////////////////////////////////////

                    // This account was never activated through its activation link. It can sign in
                    // because completing a password reset proves the same mailbox and therefore also
                    // activates — see `EmailAndPasswordAuth.recoverAccountSetPasswordWithToken`. If
                    // that rule is ever dropped, this line goes red first.
                    api.authLogin.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = newPassword,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        val response = apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success
                        response.shouldNotBeNull()
                    }

                    api.authLogin.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = originalPassword,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.Forbidden
                    }

                    //  And the token is single-use  /////////////////////////////////////////////

                    api.authLogin.recoverAccountSetPasswordWithToken(
                        realmParam,
                        body = AuthRecoverAccountRequest.SetPasswordWithToken(
                            provider = provider,
                            token = token,
                            password = "Attacker9999!",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthRecoverAccountResponse.SetPasswordWithToken>()
                            ?.success shouldBe false
                    }
                }
            }
        }

        "A reset for an unknown address must send NO email, while answering exactly as it does for a known one" {
            apiApp {
                anonymous {
                    api.authLogin.recoverAccountInitPasswordReset(
                        realmParam,
                        body = AuthRecoverAccountRequest.InitPasswordReset(
                            provider = provider,
                            email = unknownEmail,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    // The neutral response is only half of "no account enumeration". The other half
                    // is that nothing is sent, which no response-body assertion can see.
                    //
                    // Scoped to the address rather than asserting the whole capture is empty.
                    // `CapturedEmails` is per-spec (one blueprint per spec), but it accumulates across
                    // the tests WITHIN this spec, so a global assertion would be reading the mail the
                    // first test sent. Scoping is also the more precise statement of the property.
                    emails.capturedTo(unknownEmail) shouldBe emptyList()
                }
            }
        }

        "A reset request must be case-insensitive in the email, and mail the CANONICAL address" {
            apiApp {
                anonymous {
                    emails.clear()

                    api.authLogin.recoverAccountInitPasswordReset(
                        realmParam,
                        body = AuthRecoverAccountRequest.InitPasswordReset(
                            provider = provider,
                            email = "  ${email.uppercase()}  ",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    // Before EmailAddress canonicalization this found no user and sent nothing, and
                    // the neutral response made that indistinguishable from working.
                    emails.lastTo(email).shouldNotBeNull()
                        .subject shouldBe "Funktor All Test: Recover your Account"
                }
            }
        }
    }
}
