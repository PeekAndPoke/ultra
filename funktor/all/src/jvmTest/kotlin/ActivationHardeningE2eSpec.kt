package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.AccountRetirement
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.RealmTokenConfig
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpResponse
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.auth.provider.GithubSsoAuth
import io.peekandpoke.funktor.cluster.workers.WorkersRunner
import io.peekandpoke.funktor.cluster.workers.services.WorkerRegistry
import io.peekandpoke.funktor.core.App
import io.peekandpoke.funktor.messaging.senders.CapturedEmails
import io.peekandpoke.funktor.messaging.senders.hrefs
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.funktor.testing.AppUnderTest
import io.peekandpoke.ultra.common.decodeUriComponent
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ActivationHardeningKarangoE2eSpec : ActivationHardeningE2eSpec(activationHardeningKarangoApp) {
    override val users: HardeningUsers by service(HardeningKaUsersRepo::class)
}

class ActivationHardeningMonkoE2eSpec : ActivationHardeningE2eSpec(activationHardeningMonkoApp) {
    override val users: HardeningUsers by service(HardeningMoUsersRepo::class)
}

/**
 * The activation hardening of 2026-10-19, walked through the real API on BOTH backends — auth records AND
 * users on Karango in one app, on Monko in the other, each user store with a unique index on `email`.
 *
 * Runs the PRODUCTION token config (7-day pending lifetime, 5 min / 15 min / 1 h ... resend delays) against
 * a clock the spec advances, so what is pinned is what ships.
 */
abstract class ActivationHardeningE2eSpec(app: App.Definition<FunktorAllTestConfig>) :
    AppSpec<FunktorAllTestConfig>(app) {

    abstract val users: HardeningUsers

    private val api by service(AuthApiFeature::class)
    private val authSystem by service(AuthSystem::class)
    private val emails by service(CapturedEmails::class)
    private val kronos get() = kontainer.get(Kronos::class) as Kronos.Mutable

    private val realm get() = authSystem.getRealm(ActivationHardeningRealm.REALM)
    private val records get() = authSystem.deps.storage.authRecords
    private val config = RealmTokenConfig()

    private val realmParam = RealmParam(realm = ActivationHardeningRealm.REALM)
    private val provider = EmailAndPasswordAuth.ID
    private val password = "Hardening1234!"

    private val activationLinkPrefix = "https://example.com/auth/$provider/activate/"
    private val cancelLinkPrefix = "https://example.com/auth/$provider/cancel-signup/"

    private var counter = 0

    /** Unique per test AND per run: the user collections are not fixtures, nothing clears them. */
    private fun freshEmail(tag: String) = "$tag-${System.currentTimeMillis()}-${++counter}@hardening.test"

    private fun advance(by: Duration) = kronos.mutate { advanceBy(by) }

    private suspend fun userOf(email: String): Stored<TestUser>? = users.byEmail(EmailAddress.of(email))

    private suspend fun isPending(id: String) =
        records.findLatestRecordBy(AuthRecord.PendingActivation, realm.id, UserId(id)) != null

    private suspend fun hasPassword(id: String) =
        records.findLatestRecordBy(AuthRecord.Password, realm.id, UserId(id)) != null

    //  API helpers  ////////////////////////////////////////////////////////////////////////////////////////////
    //  Extensions on AppUnderTest, each opening its own anonymous scope: `apiResponseData` lives on the app.

    private suspend fun AppUnderTest<FunktorAllTestConfig>.signUp(email: String) = anonymous {
        emails.clear()

        api.authLogin.signUp(
            realmParam,
            body = AuthSignUpRequest.EmailAndPassword(
                provider = provider, email = email, password = password, displayName = "Hardening",
            ),
        ) {
            status shouldBe HttpStatusCode.OK
            apiResponseData<AuthSignUpResponse>().shouldNotBeNull().requiresActivation shouldBe true
        }
    }

    /** The activation token from the newest mail to [email] — the one a browser would hand to the page. */
    private fun activationTokenTo(email: String): String =
        emails.lastTo(email).shouldNotBeNull().hrefs().first()
            .also { it shouldStartWith activationLinkPrefix }
            .removePrefix(activationLinkPrefix)
            .decodeUriComponent()

    /** The cancel token from the newest mail to [email] — its own secret, living as long as the account. */
    private fun cancelTokenTo(email: String): String =
        emails.lastTo(email).shouldNotBeNull().hrefs().last()
            .also { it shouldStartWith cancelLinkPrefix }
            .removePrefix(cancelLinkPrefix)
            .decodeUriComponent()

    /** One run of the expiry worker exactly as the cluster's runner calls it. */
    private suspend fun runExpiryWorker() =
        kontainer.get(PendingAccountsExpiryWorker::class).execute { WorkersRunner.State.Running }

    private suspend fun AppUnderTest<FunktorAllTestConfig>.passwordSignIn(
        email: String,
        password: String = this@ActivationHardeningE2eSpec.password,
        check: suspend AppUnderTest<FunktorAllTestConfig>.(HttpResponse) -> Unit,
    ) = anonymous {
        api.authLogin.signIn(
            realmParam,
            body = AuthSignInRequest.EmailAndPassword(provider = provider, email = email, password = password),
        ) { this@passwordSignIn.check(this) }
    }

    private suspend fun AppUnderTest<FunktorAllTestConfig>.resendToken(email: String): String {
        var token: String? = null

        passwordSignIn(email) { response ->
            token = response.apiResponseData<AuthSignInResponse>()
                .shouldBeInstanceOf<AuthSignInResponse.ActivationRequired>()
                .resendToken
        }

        return token.shouldNotBeNull()
    }

    private suspend fun AppUnderTest<FunktorAllTestConfig>.resend(token: String): AuthResendActivationResponse {
        var response: AuthResendActivationResponse? = null

        anonymous {
            api.authLogin.resendActivation(
                realmParam,
                body = AuthResendActivationRequest(provider = provider, token = token),
            ) {
                status shouldBe HttpStatusCode.OK
                response = apiResponseData<AuthResendActivationResponse>()
            }
        }

        return response.shouldNotBeNull()
    }

    private suspend fun AppUnderTest<FunktorAllTestConfig>.activate(token: String): Boolean {
        var success: Boolean? = null

        anonymous {
            api.authLogin.activateAccount(
                realmParam,
                body = AuthActivateAccountRequest(provider = provider, token = token),
            ) {
                success = apiResponseData<AuthActivateAccountResponse>()?.success
            }
        }

        return success.shouldNotBeNull()
    }

    private suspend fun AppUnderTest<FunktorAllTestConfig>.cancel(token: String): Boolean {
        var success: Boolean? = null

        anonymous {
            api.authLogin.cancelSignUp(
                realmParam,
                body = AuthCancelSignUpRequest(provider = provider, token = token),
            ) {
                status shouldBe HttpStatusCode.OK
                success = apiResponseData<AuthCancelSignUpResponse>()?.success
            }
        }

        return success.shouldNotBeNull()
    }

    private suspend fun AppUnderTest<FunktorAllTestConfig>.githubSignIn(
        code: String,
        check: suspend AppUnderTest<FunktorAllTestConfig>.(HttpResponse) -> Unit,
    ) = anonymous {
        api.authLogin.signIn(
            realmParam,
            body = AuthSignInRequest.OAuth(provider = GithubSsoAuth.ID, token = code),
        ) { this@githubSignIn.check(this) }
    }

    init {
        beforeEach { kronos.set(Kronos.systemUtc) }

        //  Password before marker  ///////////////////////////////////////////////////////////////////////////

        "A WRONG password on a pending account is a plain 403 — no ActivationRequired, no resend token" {
            // The order of the two checks IS the authorization for minting a resend token.
            apiApp {
                anonymous {
                    val email = freshEmail("wrong-password")
                    signUp(email)

                    passwordSignIn(email, password = "Wrong-password-1!") { response ->
                        response.status shouldBe HttpStatusCode.Forbidden
                        response.apiResponseData<AuthSignInResponse>().shouldBeNull()
                    }

                    val id = userOf(email).shouldNotBeNull()._id
                    records.findAllByOwner(AuthRecord.ActivationResendToken, realm.id, UserId(id)) shouldBe emptyList()
                }
            }
        }

        //  Resend: growing delays, revocation, lifetime, single use  //////////////////////////////////////////

        "Resend is throttled by GROWING delays per account, and says when the next one is possible" {
            apiApp {
                anonymous {
                    val email = freshEmail("growing")
                    signUp(email)

                    val delays = config.activationResendDelays

                    withClue("right after sign-up: too soon, and the answer names the first delay") {
                        val response = resend(resendToken(email))
                        response.sent shouldBe false
                        response.nextResendAt.shouldNotBeNull()
                    }

                    // Walk the first three delays: each resend is refused one minute early and sent once due.
                    delays.take(3).forEachIndexed { index, delay ->
                        advance(delay - 1.minutes)

                        withClue("one minute before delay #${index + 1} (${delay}) has passed") {
                            resend(resendToken(email)).sent shouldBe false
                        }

                        advance(1.minutes)
                        emails.clear()

                        withClue("once delay #${index + 1} (${delay}) has passed") {
                            val response = resend(resendToken(email))
                            response.sent shouldBe true
                            response.nextResendAt.shouldNotBeNull()
                            emails.capturedTo(email).size shouldBe 1
                        }
                    }
                }
            }
        }

        "Minting a resend token revokes the owner's earlier ones" {
            // Without this, K parallel sign-ins hold K tokens and K resends race the delay check.
            apiApp {
                anonymous {
                    val email = freshEmail("revoke")
                    signUp(email)

                    val first = resendToken(email)
                    val second = resendToken(email)

                    advance(config.activationResendDelays[0])

                    withClue("the earlier token is dead") { resend(first).sent shouldBe false }
                    withClue("the newest one still works") { resend(second).sent shouldBe true }
                }
            }
        }

        "A resend token lives activationResendTokenLifetime and not a second longer" {
            apiApp {
                anonymous {
                    val email = freshEmail("lifetime")
                    signUp(email)

                    val token = resendToken(email)

                    // Past the token's lifetime — and therefore past the first resend delay as well, so
                    // the token's expiry is the only thing that can refuse this.
                    config.activationResendTokenLifetime shouldNotBe Duration.ZERO
                    (config.activationResendTokenLifetime > config.activationResendDelays[0]) shouldBe true
                    advance(config.activationResendTokenLifetime + 1.seconds)

                    withClue("expired token") { resend(token).sent shouldBe false }
                    withClue("a fresh one at the same moment sends, so it WAS the expiry") {
                        resend(resendToken(email)).sent shouldBe true
                    }
                }
            }
        }

        //  Expiry and retirement  ///////////////////////////////////////////////////////////////////////////

        "An expired pending account is retired by the sweep: address renamed, auth records gone, row kept" {
            apiApp {
                anonymous {
                    val email = freshEmail("sweep")
                    signUp(email)
                    val token = activationTokenTo(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    withClue("one minute before expiry the sweep leaves it alone") {
                        advance(config.pendingAccountLifetime - 1.minutes)
                        runExpiryWorker()
                        userOf(email).shouldNotBeNull()._id shouldBe id
                    }

                    advance(2.minutes)
                    runExpiryWorker()

                    val retired = users.byId(id).shouldNotBeNull()

                    withClue("the row stays — other collections may reference it — under a retired address") {
                        retired.value.email.value shouldStartWith "$email#retired-"
                        retired.value.email.value shouldEndWith ".invalid"
                        AccountRetirement.isRetired(retired.value.email) shouldBe true
                        retired.value.email.isValidFormat shouldBe false
                    }

                    withClue("its password, tokens and marker are gone") {
                        hasPassword(id) shouldBe false
                        isPending(id) shouldBe false
                    }

                    withClue("the address is free and the old link is dead") {
                        userOf(email).shouldBeNull()
                        activate(token) shouldBe false
                    }
                }
            }
        }

        "The same address can sign up, expire, sign up, expire … — every retirement stays unique" {
            // Both user stores carry a UNIQUE index on email: a colliding rename would throw here.
            apiApp {
                anonymous {
                    val email = freshEmail("adam")
                    val ids = mutableListOf<String>()

                    repeat(3) {
                        signUp(email)
                        ids += userOf(email).shouldNotBeNull()._id

                        advance(config.pendingAccountLifetime + 1.minutes)
                        runExpiryWorker()
                        userOf(email).shouldBeNull()
                    }

                    val retiredEmails = ids.map { users.byId(it).shouldNotBeNull().value.email }

                    retiredEmails.toSet().size shouldBe 3
                    retiredEmails.forEach { AccountRetirement.isRetired(it) shouldBe true }
                }
            }
        }

        "Without the sweep, signing up an EXPIRED pending address retires the old account inline" {
            // Expiry must not depend on a worker having run.
            apiApp {
                anonymous {
                    val email = freshEmail("inline")
                    signUp(email)
                    val oldId = userOf(email).shouldNotBeNull()._id

                    withClue("before expiry the address is still taken") {
                        advance(config.pendingAccountLifetime - 1.minutes)
                        api.authLogin.signUp(
                            realmParam,
                            body = AuthSignUpRequest.EmailAndPassword(
                                provider = provider, email = email, password = password, displayName = "Again",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }

                    advance(2.minutes)
                    signUp(email)

                    val newUser = userOf(email).shouldNotBeNull()
                    newUser._id shouldNotBe oldId
                    AccountRetirement.isRetired(users.byId(oldId).shouldNotBeNull().value.email) shouldBe true
                }
            }
        }

        "A link resent just before expiry dies WITH the account" {
            apiApp {
                anonymous {
                    val email = freshEmail("late-link")
                    signUp(email)

                    advance(config.pendingAccountLifetime - 1.hours)
                    resend(resendToken(email)).sent shouldBe true
                    val lateToken = activationTokenTo(email)

                    // Well inside the link's own 24 h, but past the account's 7 days.
                    advance(2.hours)

                    activate(lateToken) shouldBe false
                    withClue("and an expired account is not re-mailed either") {
                        resend(resendToken(email)).sent shouldBe false
                    }
                }
            }
        }

        //  Cancel  /////////////////////////////////////////////////////////////////////////////////////////////

        "The mailed cancel link retires the pending account, once, and frees the address" {
            apiApp {
                anonymous {
                    val email = freshEmail("cancel")
                    signUp(email)
                    val activationToken = activationTokenTo(email)
                    val token = cancelTokenTo(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    cancel(token) shouldBe true

                    val retired = users.byId(id).shouldNotBeNull().value.email
                    AccountRetirement.isRetired(retired) shouldBe true
                    hasPassword(id) shouldBe false
                    isPending(id) shouldBe false

                    withClue("single-use, and the activation link died with the account") {
                        cancel(token) shouldBe false
                        activate(activationToken) shouldBe false
                    }

                    withClue("the pre-registering password is gone: not even the RETIRED address signs in with it") {
                        passwordSignIn(retired.value) { response ->
                            response.status shouldBe HttpStatusCode.Forbidden
                            response.apiResponseData<AuthSignInResponse>().shouldBeNull()
                        }
                    }

                    withClue("the address is free") { signUp(email) }
                }
            }
        }

        "Cancel never retires an ACTIVATED account" {
            // A password reset activates without consuming the activation token, so the token is still
            // live here and only the account's state can refuse.
            apiApp {
                anonymous {
                    val email = freshEmail("cancel-active")
                    signUp(email)
                    // The cancel token outlives an activation (it expires with the pending lifetime), so only
                    // the account's state can refuse here.
                    val cancelToken = cancelTokenTo(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    emails.clear()
                    api.authLogin.recoverAccountInitPasswordReset(
                        realmParam,
                        body = AuthRecoverAccountRequest.InitPasswordReset(provider = provider, email = email),
                    ) { status shouldBe HttpStatusCode.OK }

                    val resetToken = emails.lastTo(email).shouldNotBeNull().hrefs().single()
                        .substringAfterLast("/").decodeUriComponent()

                    api.authLogin.recoverAccountSetPasswordWithToken(
                        realmParam,
                        body = AuthRecoverAccountRequest.SetPasswordWithToken(
                            provider = provider, token = resetToken, password = "Brand-new-1234!",
                        ),
                    ) { status shouldBe HttpStatusCode.OK }

                    isPending(id) shouldBe false

                    cancel(cancelToken) shouldBe false

                    users.byId(id).shouldNotBeNull().value.email shouldBe EmailAddress.of(email)
                    passwordSignIn(email, password = "Brand-new-1234!") { response ->
                        response.apiResponseData<AuthSignInResponse>().shouldBeInstanceOf<AuthSignInResponse.Success>()
                    }
                }
            }
        }

        "A retired address is answered like an unknown one: no reset mail" {
            apiApp {
                anonymous {
                    val email = freshEmail("retired-reset")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id
                    cancel(cancelTokenTo(email)) shouldBe true

                    val retired = users.byId(id).shouldNotBeNull().value.email

                    emails.clear()
                    api.authLogin.recoverAccountInitPasswordReset(
                        realmParam,
                        body = AuthRecoverAccountRequest.InitPasswordReset(provider = provider, email = retired.value),
                    ) { status shouldBe HttpStatusCode.OK }

                    emails.capturedTo(retired.value) shouldBe emptyList()
                    emails.capturedTo(email) shouldBe emptyList()

                    withClue("not even a reset token is issued: the mailer's own refusal is a second layer, not the first") {
                        records.findLatestRecordBy(AuthRecord.PasswordRecoveryToken, realm.id, UserId(id)).shouldBeNull()
                    }
                }
            }
        }

        "The cancel link works for the account's WHOLE pending life, long after the activation link died" {
            apiApp {
                anonymous {
                    val email = freshEmail("cancel-day-6")
                    signUp(email)
                    val activationToken = activationTokenTo(email)
                    val cancelToken = cancelTokenTo(email)

                    advance(6.days)

                    withClue("the 24 h activation link is long dead") { activate(activationToken) shouldBe false }
                    withClue("the victim can still decline") { cancel(cancelToken) shouldBe true }
                    userOf(email).shouldBeNull()
                }
            }
        }

        "The cancel link dies WITH the account" {
            apiApp {
                anonymous {
                    val email = freshEmail("cancel-day-8")
                    signUp(email)
                    val cancelToken = cancelTokenTo(email)

                    advance(config.pendingAccountLifetime + 1.minutes)

                    cancel(cancelToken) shouldBe false
                }
            }
        }

        //  Expired = retired at the point of use  ////////////////////////////////////////////////////////////

        "A reset of an EXPIRED pending account retires it and answers like an unknown address" {
            // Otherwise a reset would activate an account that outlived its sign-up — sweep or no sweep.
            apiApp {
                anonymous {
                    val email = freshEmail("expired-reset")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    advance(config.pendingAccountLifetime + 1.minutes)
                    emails.clear()

                    api.authLogin.recoverAccountInitPasswordReset(
                        realmParam,
                        body = AuthRecoverAccountRequest.InitPasswordReset(provider = provider, email = email),
                    ) { status shouldBe HttpStatusCode.OK }

                    emails.capturedTo(email) shouldBe emptyList()
                    userOf(email).shouldBeNull()
                    AccountRetirement.isRetired(users.byId(id).shouldNotBeNull().value.email) shouldBe true
                }
            }
        }

        "Verified SSO on an EXPIRED pending account: sign-in finds nothing, sign-up gets a FRESH account" {
            apiApp {
                anonymous {
                    val email = freshEmail("expired-sso")
                    signUp(email)
                    val oldId = userOf(email).shouldNotBeNull()._id

                    advance(config.pendingAccountLifetime + 1.minutes)

                    githubSignIn(FakeGithub.verified(email)) { response ->
                        response.status shouldBe HttpStatusCode.Forbidden
                    }

                    withClue("the expired account was retired, not activated") {
                        userOf(email).shouldBeNull()
                        isPending(oldId) shouldBe false
                        hasPassword(oldId) shouldBe false
                    }

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.OAuth(provider = GithubSsoAuth.ID, token = FakeGithub.verified(email)),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthSignUpResponse>().shouldNotBeNull().signIn.shouldNotBeNull()
                    }

                    userOf(email).shouldNotBeNull()._id shouldNotBe oldId
                }
            }
        }

        "The Funktor module registers the expiry worker, so every app on funktor/all retires expired sign-ups" {
            kontainer.get(WorkerRegistry::class).workerClasses shouldContain PendingAccountsExpiryWorker::class
        }

        //  SSO on a pending account  ///////////////////////////////////////////////////////////////////////////

        "SSO with a VERIFIED address activates a pending account and DELETES the sign-up password" {
            // The pre-registration takeover: an attacker signs up the victim's address with a password
            // they know; the victim later signs in with GitHub. The attacker's password must never work.
            apiApp {
                anonymous {
                    val email = freshEmail("victim")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    githubSignIn(FakeGithub.verified(email)) { response ->
                        response.status shouldBe HttpStatusCode.OK
                        response.apiResponseData<AuthSignInResponse>().shouldBeInstanceOf<AuthSignInResponse.Success>()
                    }

                    isPending(id) shouldBe false
                    hasPassword(id) shouldBe false

                    withClue("the pre-registered password is dead: a plain 403, not even ActivationRequired") {
                        passwordSignIn(email) { response ->
                            response.status shouldBe HttpStatusCode.Forbidden
                            response.apiResponseData<AuthSignInResponse>().shouldBeNull()
                        }
                    }
                }
            }
        }

        "SSO with an UNVERIFIED address on a pending account is refused and changes nothing" {
            apiApp {
                anonymous {
                    val email = freshEmail("unverified")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    githubSignIn(FakeGithub.unverified(email)) { response ->
                        response.status shouldBe HttpStatusCode.Forbidden
                    }

                    isPending(id) shouldBe true
                    hasPassword(id) shouldBe true
                }
            }
        }

        "SSO sign-UP on an existing pending account follows the same rule" {
            apiApp {
                anonymous {
                    val email = freshEmail("sso-signup")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.OAuth(provider = GithubSsoAuth.ID, token = FakeGithub.unverified(email)),
                    ) {
                        status shouldBe HttpStatusCode.BadRequest
                    }

                    isPending(id) shouldBe true

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.OAuth(provider = GithubSsoAuth.ID, token = FakeGithub.verified(email)),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthSignUpResponse>().shouldNotBeNull().signIn.shouldNotBeNull()
                    }

                    isPending(id) shouldBe false
                    hasPassword(id) shouldBe false
                    userOf(email).shouldNotBeNull()._id shouldBe id
                }
            }
        }
    }
}
