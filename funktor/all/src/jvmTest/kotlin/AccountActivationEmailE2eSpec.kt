package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.senders.hrefs
import io.peekandpoke.ultra.common.decodeUriComponent
import io.peekandpoke.ultra.common.encodeUriComponent
import io.peekandpoke.ultra.vault.value

/**
 * Walks account activation the way a user does: sign up, READ THE EMAIL, follow the link in it, and
 * only then get in.
 *
 * The middle step is the point, exactly as in [PasswordResetEmailE2eSpec]. Every piece of this flow
 * can be unit-tested green while the feature is completely broken for the user: a sign-up that mails
 * nothing, or mails a link whose token does not survive URL encoding, is indistinguishable from a
 * working one from the server's side.
 *
 * The security property being pinned is narrow and load-bearing: **correct credentials are not enough
 * until the address is proven.** Before this feature `AuthRealm.signUp` handed out a session
 * immediately, which is what made the SSO pre-hijack chain in
 * `.claude/tasks/20260726-sso-email-verification.md` work.
 *
 * SCOPE: [TestUserRealm] is org-less, so the closing sign-in leg does not generalize to an
 * `OrgPolicy.Required` realm (there `issueSignIn` answers `OrgSelectionRequired`). The activation half
 * does generalize — the marker and the token carry no org dimension.
 */
class AccountActivationEmailE2eSpec : FunktorApiSpec() {

    private val api by service(AuthApiFeature::class)
    private val emails get() = capturedEmails

    private val realmParam = RealmParam(realm = TestUserRealm.REALM)
    private val provider = EmailAndPasswordAuth.ID

    // Unique per run: the test users repo is not a fixture, so it is not cleared between runs.
    private val email = "activate-${System.currentTimeMillis()}@test.com"
    private val password = "Activate1234!"

    /** The link the realm builds, up to but excluding the token — see [AuthFrontendRoutes.activateAccount]. */
    private val activationLinkPrefix = "https://example.com/auth/$provider/activate/"

    init {
        "Account activation must be walkable end to end, through the email" {
            apiApp {
                anonymous {
                    emails.clear()

                    //  When a new account is created  //////////////////////////////////////////

                    api.auth.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                            displayName = "Activate Me",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK

                        val response = apiResponseData<AuthSignUpResponse>().shouldNotBeNull()

                        // The sign-up SUCCEEDED and still issued no session. Both halves matter:
                        // `success == false` would make the frontend render "Sign-up failed", and a
                        // non-null `signIn` would mean an unproven address got a live session — the
                        // exact hole this feature closes.
                        response.success shouldBe true
                        response.requiresActivation shouldBe true
                        response.signIn shouldBe null
                    }

                    //  Then an activation mail actually goes out ...  //////////////////////////

                    val activationMail = emails.lastTo(email).shouldNotBeNull()

                    activationMail.subject shouldBe "Funktor All Test: Activate your Account"
                    activationMail.source shouldBe "test@example.com"

                    // `singleOrNull` is a real assertion: an activation mail should carry ONE link and
                    // nothing else — no support link to be confused with the real one.
                    val link = activationMail.hrefs().singleOrNull().shouldNotBeNull()

                    link shouldStartWith activationLinkPrefix

                    val tokenSegment = link.removePrefix(activationLinkPrefix)

                    // The token is random base64, so '+', '/' and '=' are all routine in it, and the
                    // frontend route is `/auth/{provider}/activate/{token}` — ONE path segment. Left
                    // unencoded it splits across segments at every '/', the route stops matching, and
                    // the link is dead while every server-side test still passes. Decoding alone does
                    // not notice: undoing an encoding that never happened is a no-op on base64.
                    tokenSegment shouldNotContain "/"
                    tokenSegment shouldBe tokenSegment.decodeUriComponent().encodeUriComponent()

                    // Decoding here is what a browser does before handing the value to the route.
                    val token = tokenSegment.decodeUriComponent()

                    //  ... and the PERSISTED copy must not carry the token  ////////////////////

                    // Same reason as the reset mail: the sent-messages inspector shows stored mail to
                    // support staff, and a live activation token in there is an account handed over.
                    val stored = sentMessages.findByRefs(setOf(email)).toList().map { it.value() }
                        .filter { "account-activation" in it.tags }

                    withClue("the activation mail must be persisted, otherwise this asserts nothing") {
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
                    storedBody shouldContain "Click the link below to activate your account"

                    //  Until the link is followed, the account cannot sign in  /////////////////

                    api.auth.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                        ),
                    ) {
                        // The password is CORRECT here. Anything less would pass for the wrong reason.
                        status shouldBe HttpStatusCode.Forbidden
                    }

                    //  The token from the mail activates  //////////////////////////////////////

                    api.auth.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(provider = provider, token = token),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe true
                    }

                    //  ... and now the same credentials work  //////////////////////////////////

                    api.auth.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        (apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success)
                            .shouldNotBeNull()
                    }

                    //  And the token is single-use  ////////////////////////////////////////////

                    api.auth.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(provider = provider, token = token),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe false
                    }
                }
            }
        }

        "An unknown activation token must not activate anything" {
            apiApp {
                anonymous {
                    api.auth.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(provider = provider, token = "no-such-token"),
                    ) {
                        // OK rather than an error: the endpoint is anonymous, so a different answer
                        // for a token that exists would let an attacker probe for live tokens.
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe false
                    }
                }
            }
        }
    }
}
