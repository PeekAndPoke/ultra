package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.senders.hrefs
import io.peekandpoke.funktor.testing.AppUnderTest
import io.peekandpoke.ultra.common.decodeUriComponent
import io.peekandpoke.ultra.common.encodeUriComponent
import io.peekandpoke.ultra.security.user.EmailAddress
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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

    /**
     * The token a browser would hand to the route, taken out of a real mail.
     *
     * `single()` is an assertion: an activation mail carries ONE link and nothing else. The URL
     * ENCODING of the token is pinned separately, in the first test.
     */
    private fun activationTokenFrom(mail: Email): String = mail.hrefs().single()
        .removePrefix(activationLinkPrefix)
        .decodeUriComponent()

    /** Read from the realm, so the waits below cannot drift out of step with the configured window. */
    private val realmTokenConfig = TestUserRealm.TOKEN_CONFIG

    /**
     * The resend authorization, obtained the ONLY way it can be: by signing in with the right password
     * and being refused because the account is pending.
     *
     * That this helper needs a password at all is the property under test elsewhere — an anonymous
     * caller who merely knows the address cannot get one.
     */
    private suspend fun AppUnderTest<FunktorAllTestConfig>.resendTokenFor(
        scope: AppUnderTest<FunktorAllTestConfig>.AuthenticationScope,
        email: String,
    ): String = with(scope) {
        var token: String? = null

        api.authLogin.signIn(
            realmParam,
            body = AuthSignInRequest.EmailAndPassword(
                provider = provider, email = email, password = password,
            ),
        ) {
            token = apiResponseData<AuthSignInResponse>()
                .shouldBeInstanceOf<AuthSignInResponse.ActivationRequired>()
                .resendToken
        }

        return token.shouldNotBeNull()
    }

    init {
        "Account activation must be walkable end to end, through the email" {
            apiApp {
                anonymous {
                    emails.clear()

                    //  When a new account is created  //////////////////////////////////////////

                    api.authLogin.signUp(
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

                    api.authLogin.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                        ),
                    ) {
                        // The password is CORRECT here. Anything less would pass for the wrong reason.
                        //
                        // The answer is a response CASE, not an error: the credential check passed and
                        // there is a defined next step. A wrong password still gets a 403, so this
                        // cannot be satisfied by any ordinary failure.
                        status shouldBe HttpStatusCode.OK

                        apiResponseData<AuthSignInResponse>()
                            .shouldBeInstanceOf<AuthSignInResponse.ActivationRequired>()
                    }

                    //  The token from the mail activates  //////////////////////////////////////

                    api.authLogin.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(provider = provider, token = token),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe true
                    }

                    //  ... and now the same credentials work  //////////////////////////////////

                    api.authLogin.signIn(
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

                    api.authLogin.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(provider = provider, token = token),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe false
                    }
                }
            }
        }

        "A resend must deliver a WORKING new link, and kill the old one" {
            // The lockout this prevents: the first mail is lost or the 24h link lapses, and without a
            // resend the only way back is a password reset the user has no reason to think of.
            apiApp {
                anonymous {
                    val email = "resend-${System.currentTimeMillis()}@test.com"

                    emails.clear()

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                            displayName = "Resend Me",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    val firstToken = activationTokenFrom(emails.lastTo(email).shouldNotBeNull())

                    // Sign-up has just issued a token, so the cooldown is running. Waiting it out is
                    // the point: it proves the window OPENS again rather than blocking forever, which
                    // a mocked clock would not. `TestUserRealm` shortens it to 2s for exactly this.
                    // The wait can only LENGTHEN the elapsed time, so a slow machine cannot flip what
                    // this test means.
                    delay(realmTokenConfig.activationResendCooldown + 500.milliseconds)

                    // The resend authorization comes from a refused sign-in, and nowhere else — an
                    // anonymous caller naming an address cannot reach this endpoint at all.
                    val resendToken = resendTokenFor(this, email)

                    emails.clear()

                    api.authLogin.resendActivation(
                        realmParam,
                        body = AuthResendActivationRequest(provider = provider, token = resendToken),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthResendActivationResponse>()?.sent shouldBe true
                    }

                    val secondToken = activationTokenFrom(emails.lastTo(email).shouldNotBeNull())

                    withClue("the resent link must be a NEW token, not the same one mailed again") {
                        secondToken shouldNotBe firstToken
                    }

                    // Rotation: the superseded link must be dead. Otherwise every resend leaves
                    // another live credential in another inbox copy, for as long as its 24h lasts.
                    api.authLogin.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(provider = provider, token = firstToken),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe false
                    }

                    // And the account is still blocked, i.e. the dead link really did nothing.
                    api.authLogin.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider, email = email, password = password,
                        ),
                    ) {
                        apiResponseData<AuthSignInResponse>()
                            .shouldBeInstanceOf<AuthSignInResponse.ActivationRequired>()
                    }

                    api.authLogin.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(provider = provider, token = secondToken),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe true
                    }

                    api.authLogin.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider, email = email, password = password,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        (apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success)
                            .shouldNotBeNull()
                    }
                }
            }
        }

        "A resend without a valid token must send nothing — naming an address is not enough" {
            // THE property that keeps this endpoint from being an anonymous mail primitive aimed at
            // any mailbox. It is what the first cut of this feature got wrong: it took an email.
            apiApp {
                anonymous {
                    val email = "unauthorized-${System.currentTimeMillis()}@test.com"

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                            displayName = "No Token",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    // A real pending account exists, the caller knows the address, and the cooldown is
                    // irrelevant here — without the token there is still nothing they can cause.
                    delay(realmTokenConfig.activationResendCooldown + 500.milliseconds)

                    emails.clear()

                    api.authLogin.resendActivation(
                        realmParam,
                        body = AuthResendActivationRequest(provider = provider, token = "not-a-real-token"),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthResendActivationResponse>()?.sent shouldBe false
                    }

                    emails.capturedTo(email) shouldBe emptyList()
                }
            }
        }

        "A resend token must be single-use, so one sign-in buys exactly one resend" {
            apiApp {
                anonymous {
                    val email = "single-use-${System.currentTimeMillis()}@test.com"

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                            displayName = "Single Use",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    delay(realmTokenConfig.activationResendCooldown + 500.milliseconds)

                    val resendToken = resendTokenFor(this, email)

                    api.authLogin.resendActivation(
                        realmParam,
                        body = AuthResendActivationRequest(provider = provider, token = resendToken),
                    ) {
                        apiResponseData<AuthResendActivationResponse>()?.sent shouldBe true
                    }

                    emails.clear()

                    // Wait the cooldown out FIRST, so single-use is the only thing that can stop the
                    // replay. Without this wait the test passes even when the token is never consumed,
                    // because the cooldown blocks the second send — verified by mutation: dropping
                    // `removeAuthRecord` left the suite green until this delay was added.
                    delay(realmTokenConfig.activationResendCooldown + 500.milliseconds)

                    api.authLogin.resendActivation(
                        realmParam,
                        body = AuthResendActivationRequest(provider = provider, token = resendToken),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthResendActivationResponse>()?.sent shouldBe false
                    }

                    emails.capturedTo(email) shouldBe emptyList()
                }
            }
        }

        "A resend inside the cooldown must send nothing, even with a valid token" {
            // THE throttle behind the authorization: re-authenticating in a loop must not mail faster
            // than the window. Sign-up has just issued a token, so this resend is inside it — and the
            // test does NOT wait, so a slow machine can only make the elapsed time approach the window
            // and fail LOUDLY, never silently exercise a different path.
            apiApp {
                anonymous {
                    val email = "cooldown-${System.currentTimeMillis()}@test.com"

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = email,
                            password = password,
                            displayName = "Cool Down",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    val resendToken = resendTokenFor(this, email)

                    emails.clear()

                    api.authLogin.resendActivation(
                        realmParam,
                        body = AuthResendActivationRequest(provider = provider, token = resendToken),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthResendActivationResponse>()?.sent shouldBe false
                    }

                    emails.capturedTo(email) shouldBe emptyList()
                }
            }
        }

        "A resend must send nothing for an ALREADY-ACTIVATED account" {
            apiApp {
                anonymous {
                    val activated = "already-active-${System.currentTimeMillis()}@test.com"

                    emails.clear()

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = activated,
                            password = password,
                            displayName = "Already Active",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    // Grab the resend authorization BEFORE activating, so the token is valid and the
                    // only thing stopping the mail is the account's state.
                    val resendToken = resendTokenFor(this, activated)

                    api.authLogin.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(
                            provider = provider,
                            token = activationTokenFrom(emails.lastTo(activated).shouldNotBeNull()),
                        ),
                    ) {
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe true
                    }

                    delay(realmTokenConfig.activationResendCooldown + 500.milliseconds)

                    emails.clear()

                    api.authLogin.resendActivation(
                        realmParam,
                        body = AuthResendActivationRequest(provider = provider, token = resendToken),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthResendActivationResponse>()?.sent shouldBe false
                    }

                    emails.capturedTo(activated) shouldBe emptyList()
                }
            }
        }

        "A user whose language is German must get the GERMAN activation mail, and its link must work" {
            // The whole localized-email pipeline in one walk: the user's stored language selects a
            // whole rendering, its subject and body come out together, and the link is still one the
            // server accepts.
            //
            // The link leg is not decoration. The URL goes into an `href` through kotlinx.html's
            // attribute escaping and comes back out through `hrefs()`' entity decoding, and a base64
            // token routinely contains `+`, `/` and `=`. A mismatch between those two steps mangles
            // the token while leaving a mail that looks perfectly fine — the failure no unit test on
            // either side can see.
            apiApp {
                anonymous {
                    val german = "german-${System.currentTimeMillis()}@test.com"

                    emails.clear()

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(
                            provider = provider,
                            email = german,
                            password = password,
                            displayName = "Deutscher Nutzer",
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    // Sign-up mails in the realm default (en) — nothing can express a language before
                    // the account exists. Asserting it here pins that the SWITCH below is what changes
                    // the mail, not some incidental default.
                    emails.lastTo(german).shouldNotBeNull()
                        .subject shouldBe "Funktor All Test: Activate your Account"

                    val stored = realm.users.loadByEmail(EmailAddress.of(german)).shouldNotBeNull()

                    usersRepo.save(
                        stored.modify { it.copy(language = LanguageSettings(messaging = "de-CH")) }
                    )

                    // `de-CH` deliberately, not `de`: there is no Swiss template, so this also proves
                    // the fallback chain walks region -> base language rather than dropping straight to
                    // the `en` fallback.

                    delay(realmTokenConfig.activationResendCooldown + 500.milliseconds)

                    val resendToken = resendTokenFor(this, german)

                    emails.clear()

                    api.authLogin.resendActivation(
                        realmParam,
                        body = AuthResendActivationRequest(provider = provider, token = resendToken),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthResendActivationResponse>()?.sent shouldBe true
                    }

                    val germanMail = emails.lastTo(german).shouldNotBeNull()

                    //  Subject AND body come from the same template  ///////////////////////////

                    germanMail.subject shouldBe "Funktor All Test: Konto aktivieren"

                    val body = germanMail.body.content

                    body shouldContain "Willkommen!"
                    body shouldContain "Konto aktivieren"

                    // No mixed-language mail: the English template must not be leaking in alongside
                    // the German one. Whole-template resolution is what guarantees this, and the
                    // assertion is what stops a future fragment-assembly "improvement" from passing.
                    body shouldNotContain "Welcome!"
                    body shouldNotContain "Activate account"

                    //  ... and the German link still activates  ////////////////////////////////

                    api.authLogin.activateAccount(
                        realmParam,
                        body = AuthActivateAccountRequest(
                            provider = provider,
                            token = activationTokenFrom(germanMail),
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthActivateAccountResponse>()?.success shouldBe true
                    }

                    api.authLogin.signIn(
                        realmParam,
                        body = AuthSignInRequest.EmailAndPassword(
                            provider = provider, email = german, password = password,
                        ),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        (apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success)
                            .shouldNotBeNull()
                    }
                }
            }
        }

        "An unknown activation token must not activate anything" {
            apiApp {
                anonymous {
                    api.authLogin.activateAccount(
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
