package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse.Failure
import io.peekandpoke.funktor.auth.model.AuthDisplayName
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.core.broker.typedRouteRenderer
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.senders.hrefs
import io.peekandpoke.funktor.testing.AppUnderTest
import io.peekandpoke.ultra.common.decodeUriComponent
import io.peekandpoke.ultra.common.encodeUriComponent
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.security.user.EmailAddress
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.milliseconds

/**
 * Walks account activation the way a user does: sign up with the address only, READ THE EMAIL, follow the link
 * in it, choose the password there, and only then get in (email first, ULTRA-00231).
 *
 * The middle step is the point, exactly as in [PasswordResetEmailE2eSpec]. Every piece of this flow
 * can be unit-tested green while the feature is completely broken for the user: a sign-up that mails
 * nothing, or mails a link whose token does not survive URL encoding, is indistinguishable from a
 * working one from the server's side.
 *
 * The security property being pinned is narrow and load-bearing: **an account that has not proven its address
 * holds no credential.** Before account activation `AuthRealm.signUp` handed out a session immediately, which is
 * what made the SSO pre-hijack chain in ULTRA-00118 (sso email verification) work; before email first, the
 * account at least held the sign-up password, which a pre-registering attacker chose.
 *
 * SCOPE: [TestUserRealm] is org-less, so the closing sign-in leg does not generalize to an
 * `OrgPolicy.Required` realm (there `issueSignIn` answers `OrgSelectionRequired`), and neither does the
 * session the activation POST hands out, which comes from the same `issueSignIn`.
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

    /** The "Didn't sign up? Cancel" link — see [AuthFrontendRoutes.cancelSignUp]. */
    private val cancelLinkPrefix = "https://example.com/auth/$provider/cancel-signup/"

    /**
     * The token a browser would hand to the route, taken out of a real mail.
     *
     * The destructuring is an assertion: an activation mail carries exactly the activation link and the
     * cancel link, in that order, and nothing else. The URL ENCODING of the token is pinned separately,
     * in the first test.
     */
    private fun activationTokenFrom(mail: Email): String {
        val (activate, cancel) = mail.hrefs().also { it.size shouldBe 2 }

        activate shouldStartWith activationLinkPrefix
        cancel shouldStartWith cancelLinkPrefix

        return activate.removePrefix(activationLinkPrefix).decodeUriComponent()
    }

    /** Read from the realm, so the waits below cannot drift out of step with the configured window. */
    private val realmTokenConfig = TestUserRealm.TOKEN_CONFIG

    /** Signs [email] up — the address only, no password and no name — and returns the answer. */
    private suspend fun AppUnderTest<FunktorAllTestConfig>.signUp(
        scope: AppUnderTest<FunktorAllTestConfig>.AuthenticationScope,
        email: String,
    ): AuthSignUpResponse = with(scope) {
        var response: AuthSignUpResponse? = null

        api.authLogin.signUp(
            realmParam,
            body = AuthSignUpRequest.EmailAndPassword(provider = provider, email = email),
        ) {
            status shouldBe HttpStatusCode.OK
            response = apiResponseData<AuthSignUpResponse>()
        }

        response.shouldNotBeNull()
    }

    /** Chooses [password] and [displayName] through the mailed [token]; returns the answer and its status. */
    private suspend fun AppUnderTest<FunktorAllTestConfig>.activate(
        scope: AppUnderTest<FunktorAllTestConfig>.AuthenticationScope,
        token: String,
        password: String = this@AccountActivationEmailE2eSpec.password,
        displayName: String = "Activate Me",
    ): Pair<HttpStatusCode, AuthActivateAccountResponse> = with(scope) {
        var answer: Pair<HttpStatusCode, AuthActivateAccountResponse>? = null

        api.authLogin.activateAccount(
            realmParam,
            body = AuthActivateAccountRequest(
                provider = provider, token = token, password = password, displayName = displayName,
            ),
        ) {
            val answered = apiResponseData<AuthActivateAccountResponse>().shouldNotBeNull()

            // The server writes with Slumber, the browser reads with kotlinx: both must read the same answer.
            withClue("the answer as the browser decodes it") {
                val inBrowser = browserJson.decodeFromString(AuthActivateAccountResponse.serializer().api(), bodyAsText())
                    .data.shouldNotBeNull()

                inBrowser.success shouldBe answered.success
                inBrowser.failure shouldBe answered.failure
            }

            answer = status to answered
        }

        answer.shouldNotBeNull()
    }

    /**
     * Decodes the way an app's browser `ApiClient` does (as the funktor-demo apps configure it): kotlinx, which honours
     * `@SerialName` where Slumber does not.
     */
    private val browserJson = Json {
        classDiscriminator = "_type"
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Signs in with [password]; returns the session, or null when refused. */
    private suspend fun AppUnderTest<FunktorAllTestConfig>.signInWith(
        scope: AppUnderTest<FunktorAllTestConfig>.AuthenticationScope,
        email: String,
        password: String = this@AccountActivationEmailE2eSpec.password,
    ): AuthSignInResponse.Success? = with(scope) {
        var session: AuthSignInResponse.Success? = null

        api.authLogin.signIn(
            realmParam,
            body = AuthSignInRequest.EmailAndPassword(provider = provider, email = email, password = password),
        ) {
            session = apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success
        }

        session
    }

    init {
        "Account activation must be walkable end to end, through the email" {
            apiApp {
                anonymous {
                    emails.clear()

                    //  When a new account is created  //////////////////////////////////////////

                    val response = signUp(this, email)

                    // The sign-up SUCCEEDED and still issued no session. Both halves matter:
                    // `success == false` would make the frontend render "Sign-up failed", and a
                    // non-null `signIn` would mean an unproven address got a live session — the
                    // exact hole this feature closes.
                    response.success shouldBe true
                    response.requiresActivation shouldBe true
                    response.signIn shouldBe null

                    //  Then an activation mail actually goes out ...  //////////////////////////

                    val activationMail = emails.lastTo(email).shouldNotBeNull()

                    activationMail.subject shouldBe "Funktor All Test: Activate your Account"
                    activationMail.source shouldBe "test@example.com"

                    // Exactly two links: activate, and the way out for someone who did not sign up.
                    // Nothing else — no support link to be confused with the real ones.
                    val (link, cancelLink) = activationMail.hrefs().also { it.size shouldBe 2 }

                    link shouldStartWith activationLinkPrefix

                    withClue("the cancel link carries its OWN token, which lives as long as the account") {
                        cancelLink shouldStartWith cancelLinkPrefix
                        cancelLink.removePrefix(cancelLinkPrefix) shouldNotBe link.removePrefix(activationLinkPrefix)
                    }

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
                    storedBody shouldContain "Click the link below to choose your password"

                    //  Until the link is followed, there is nothing to sign in with  ///////////

                    withClue("email first: the pending account holds no password, so no password signs in") {
                        signInWith(this, email).shouldBeNull()
                    }

                    //  The token from the mail chooses the password, activates, and signs in  //

                    val (status, activated) = activate(this, token)

                    status shouldBe HttpStatusCode.OK
                    activated.success shouldBe true
                    activated.signIn.shouldBeInstanceOf<AuthSignInResponse.Success>()

                    //  ... and now the chosen password works  //////////////////////////////////

                    signInWith(this, email).shouldNotBeNull()

                    //  And the token is single-use: a replay sets NO new password  /////////////

                    activate(this, token, password = "Replayed1234!").second.success shouldBe false

                    withClue("a replayed link must not have changed the password") {
                        signInWith(this, email, password = "Replayed1234!").shouldBeNull()
                        signInWith(this, email).shouldNotBeNull()
                    }
                }
            }
        }

        "Signing up again must deliver a WORKING new link, and with NO overlap configured kill the old one" {
            // The lockout this prevents: the first mail is lost or the 24h link lapses. With no password at
            // sign-up there is no sign-in to ask from, so signing up again with the same address is the way.
            apiApp {
                anonymous {
                    val email = "resend-${System.currentTimeMillis()}@test.com"

                    emails.clear()

                    val fresh = signUp(this, email)

                    val firstToken = activationTokenFrom(emails.lastTo(email).shouldNotBeNull())

                    // Sign-up has just sent a mail, so the first delay is running. Waiting it out is the
                    // point: it proves the window OPENS again rather than blocking forever. `TestUserRealm`
                    // shortens it to 2s for exactly this. The wait can only LENGTHEN the elapsed time, so a
                    // slow machine cannot flip what this test means.
                    delay(realmTokenConfig.activationResendDelays[0] + 500.milliseconds)

                    emails.clear()

                    withClue("signing up again answers exactly like a fresh sign-up") {
                        signUp(this, email) shouldBe fresh
                    }

                    val secondToken = activationTokenFrom(emails.lastTo(email).shouldNotBeNull())

                    withClue("the re-sent link must be a NEW token, not the same one mailed again") {
                        secondToken shouldNotBe firstToken
                    }

                    // Rotation with `activationLinkOverlap = 0` (see `TestUserRealm`): the superseded link is dead at
                    // once. With the default, it would live 30 more minutes — the hardening spec pins that.
                    activate(this, firstToken).second.success shouldBe false

                    withClue("and the dead link really did nothing") {
                        signInWith(this, email).shouldBeNull()
                    }

                    activate(this, secondToken).second.success shouldBe true

                    signInWith(this, email).shouldNotBeNull()
                }
            }
        }

        "Signing up again inside the delay sends nothing, and answers exactly like a fresh sign-up" {
            // THE throttle: anyone who knows the address can sign it up again, so this must not mail faster
            // than the delay — and must not tell the caller that a delay is running, which would tell them
            // the address is pending. Sign-up has just sent a mail, so this one is inside the delay, and the
            // test does NOT wait: a slow machine can only make the elapsed time approach the delay and fail
            // LOUDLY, never silently exercise a different path.
            apiApp {
                anonymous {
                    val email = "cooldown-${System.currentTimeMillis()}@test.com"

                    val fresh = signUp(this, email)

                    emails.clear()

                    signUp(this, email) shouldBe fresh

                    emails.capturedTo(email) shouldBe emptyList()
                }
            }
        }

        "Signing up again with an ACTIVATED address sends nothing" {
            apiApp {
                anonymous {
                    val activated = "already-active-${System.currentTimeMillis()}@test.com"

                    emails.clear()

                    signUp(this, activated)

                    activate(this, activationTokenFrom(emails.lastTo(activated).shouldNotBeNull())).second.success shouldBe true

                    // Past the delay, so the account's state is the only thing stopping a mail.
                    delay(realmTokenConfig.activationResendDelays[0] + 500.milliseconds)

                    emails.clear()

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.EmailAndPassword(provider = provider, email = activated),
                    ) {
                        // The address is taken. (That this answer differs from a pending one is the enumeration
                        // asymmetry of ULTRA-00128 (signup mail throttle), unchanged here.)
                        status shouldBe HttpStatusCode.BadRequest
                    }

                    emails.capturedTo(activated) shouldBe emptyList()
                }
            }
        }

        "A WEAK password at activation is refused, and leaves the link usable" {
            apiApp {
                anonymous {
                    val email = "weak-${System.currentTimeMillis()}@test.com"

                    emails.clear()

                    signUp(this, email)

                    val token = activationTokenFrom(emails.lastTo(email).shouldNotBeNull())

                    // A plain answer naming the reason, so the page can keep its form: the input was checked before
                    // the token, so the reason tells nothing about the link.
                    activate(this, token, password = "weak") shouldBe (
                        HttpStatusCode.OK to AuthActivateAccountResponse(success = false, failure = Failure.WeakPassword)
                        )

                    withClue("still pending, nothing signs in") {
                        signInWith(this, email, password = "weak").shouldBeNull()
                    }

                    activate(this, token).second.success shouldBe true
                }
            }
        }

        "A refused NAME at activation is refused before the link, and the name chosen at activation is the account's" {
            apiApp {
                anonymous {
                    val email = "name-${System.currentTimeMillis()}@test.com"

                    emails.clear()

                    // An old client that still sends a name: the server ignores it.
                    val oldClientSignUp = buildJsonObject {
                        browserJson.parseToJsonElement(
                            restCodec.serialize(AuthSignUpRequest.EmailAndPassword(provider = provider, email = email))!!
                        ).jsonObject.forEach { (key, value) -> put(key, value) }
                        put("displayName", "Call support at +1 555 0100")
                    }

                    request(
                        HttpMethod.Post,
                        kontainer.typedRouteRenderer.render(api.authLogin.signUp.route(realmParam)),
                        setup = {
                            setBody(oldClientSignUp.toString())
                            header(HttpHeaders.ContentType, "application/json")
                        },
                    ) {
                        status shouldBe HttpStatusCode.OK
                    }

                    withClue("sign-up takes no name: the account holds the default from the address") {
                        realm.users.loadByEmail(EmailAddress.of(email)).shouldNotBeNull().value().name shouldBe
                            email.substringBefore("@")
                    }

                    val token = activationTokenFrom(emails.lastTo(email).shouldNotBeNull())

                    for (refused in listOf("", "   ", "x".repeat(AuthDisplayName.MAX_LENGTH + 1), "Call${Char(0x0A)}support")) {
                        withClue("name '$refused'") {
                            activate(this, token, displayName = refused) shouldBe (
                                HttpStatusCode.OK to AuthActivateAccountResponse(
                                    success = false, failure = Failure.InvalidDisplayName,
                                )
                                )
                        }
                    }

                    // Refused before the link, as a client error (ULTRA-00292: it was a 500 that quoted the body).
                    withClue("a body WITHOUT displayName (an old client) is refused, and the link survives it") {
                        request(
                            HttpMethod.Post,
                            kontainer.typedRouteRenderer.render(api.authLogin.activateAccount.route(realmParam)),
                            setup = {
                                setBody(
                                    buildJsonObject {
                                        put("provider", provider)
                                        put("token", token)
                                        put("password", password)
                                    }.toString()
                                )
                                header(HttpHeaders.ContentType, "application/json")
                            },
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }

                    // The link survived every refusal; the name is trimmed.
                    activate(this, token, displayName = "  Ada Lovelace ").second.success shouldBe true

                    realm.users.loadByEmail(EmailAddress.of(email)).shouldNotBeNull().value().name shouldBe "Ada Lovelace"
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

                    signUp(this, german)

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

                    delay(realmTokenConfig.activationResendDelays[0] + 500.milliseconds)

                    emails.clear()

                    signUp(this, german)

                    val germanMail = emails.lastTo(german).shouldNotBeNull()

                    //  Subject AND body come from the same template  ///////////////////////////

                    germanMail.subject shouldBe "Funktor All Test: Konto aktivieren"

                    val body = germanMail.body.content

                    body shouldContain "Willkommen!"
                    body shouldContain "Passwort wählen"

                    // No mixed-language mail: the English template must not be leaking in alongside
                    // the German one. Whole-template resolution is what guarantees this, and the
                    // assertion is what stops a future fragment-assembly "improvement" from passing.
                    body shouldNotContain "Welcome!"
                    body shouldNotContain "Choose your password"

                    //  ... and the German link still activates  ////////////////////////////////

                    activate(this, activationTokenFrom(germanMail)).second.success shouldBe true

                    signInWith(this, german).shouldNotBeNull()
                }
            }
        }

        "An unknown activation token must not activate anything" {
            apiApp {
                anonymous {
                    activate(this, "no-such-token").let { (status, answer) ->
                        // OK rather than an error: the endpoint is anonymous, so a different answer
                        // for a token that exists would let an attacker probe for live tokens.
                        status shouldBe HttpStatusCode.OK
                        answer.success shouldBe false
                    }
                }
            }
        }
    }
}
