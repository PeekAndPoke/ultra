package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
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
 * Runs the PRODUCTION token config (7-day pending lifetime, 5 min / 15 min / 1 h ... delays between re-sent mails)
 * against a clock the spec advances, so what is pinned is what ships. Since email first (ULTRA-00231) a sign-up
 * stores no password; the activation link chooses it.
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

    /** Every auth row of [id] straight from the store — expired ones included, so only a delete makes it empty. */
    private suspend fun rawRowsOf(id: String): Map<String, Int> = listOf(
        AuthRecord.Password, AuthRecord.PasswordRecoveryToken, AuthRecord.PendingActivation,
        AuthRecord.EmailVerificationToken, AuthRecord.SignUpCancelToken,
    ).associate { it.identifier to records.adapter.findAllByOwner(realm.id, it.identifier, UserId(id)).size }
        .filterValues { it > 0 }

    private suspend fun hasPassword(id: String) =
        records.findLatestRecordBy(AuthRecord.Password, realm.id, UserId(id)) != null

    //  API helpers  ////////////////////////////////////////////////////////////////////////////////////////////
    //  Extensions on AppUnderTest, each opening its own anonymous scope: `apiResponseData` lives on the app.

    /** Signs [email] up, or up AGAIN (which re-sends the link once the delay allows); clears the mails first. */
    private suspend fun AppUnderTest<FunktorAllTestConfig>.signUp(email: String) = anonymous {
        emails.clear()

        api.authLogin.signUp(
            realmParam,
            body = AuthSignUpRequest.EmailAndPassword(
                // No display name: it defaults to the address's local part, which retirement must not keep.
                provider = provider, email = email, displayName = null,
            ),
        ) {
            status shouldBe HttpStatusCode.OK
            apiResponseData<AuthSignUpResponse>().shouldNotBeNull().requiresActivation shouldBe true
        }
    }

    /** A password row as a sign-up before email first left it — the pre-registering attacker's. */
    private suspend fun seedLegacyPassword(id: String) {
        records.create(
            AuthRecord.Password(
                realm = realm.id, ownerId = UserId(id), token = authSystem.deps.passwordHasher.hashAsString(password),
            )
        )
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

    /** Chooses [password] through the activation link [token]; true when that activated the account. */
    private suspend fun AppUnderTest<FunktorAllTestConfig>.activate(
        token: String,
        password: String = this@ActivationHardeningE2eSpec.password,
    ): Boolean {
        var success: Boolean? = null

        anonymous {
            api.authLogin.activateAccount(
                realmParam,
                body = AuthActivateAccountRequest(provider = provider, token = token, password = password),
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
        beforeEach {
            kronos.set(Kronos.systemUtc)
            RetireGate.beforeRename = {}
            RetireGate.afterRetireClaim = {}
        }

        //  Email first  /////////////////////////////////////////////////////////////////////////////////////////

        "Email first: a sign-up stores NO password, so nothing signs in until the link chooses one" {
            apiApp {
                anonymous {
                    val email = freshEmail("email-first")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    isPending(id) shouldBe true
                    hasPassword(id) shouldBe false

                    passwordSignIn(email) { response ->
                        response.status shouldBe HttpStatusCode.Forbidden
                        response.apiResponseData<AuthSignInResponse>().shouldBeNull()
                    }

                    activate(activationTokenTo(email)) shouldBe true

                    isPending(id) shouldBe false
                    passwordSignIn(email) { response ->
                        response.apiResponseData<AuthSignInResponse>().shouldBeInstanceOf<AuthSignInResponse.Success>()
                    }
                }
            }
        }

        "A pending account from BEFORE email first refuses its sign-up password like a wrong one; the link replaces it" {
            // Accounts signed up under 0.114-0.117 still hold the password chosen at sign-up — possibly by a
            // pre-registering attacker. It must never sign in, and the mailbox owner's choice must replace it.
            apiApp {
                anonymous {
                    val email = freshEmail("legacy-pending")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id
                    seedLegacyPassword(id)

                    passwordSignIn(email) { response ->
                        response.status shouldBe HttpStatusCode.Forbidden
                        response.apiResponseData<AuthSignInResponse>().shouldBeNull()
                    }

                    activate(activationTokenTo(email), password = "Victim-chose-1!") shouldBe true

                    withClue("the old password is gone, the chosen one signs in") {
                        records.findAllByOwner(AuthRecord.Password, realm.id, UserId(id)).size shouldBe 1
                        passwordSignIn(email) { response -> response.status shouldBe HttpStatusCode.Forbidden }
                        passwordSignIn(email, password = "Victim-chose-1!") { response ->
                            response.apiResponseData<AuthSignInResponse>().shouldBeInstanceOf<AuthSignInResponse.Success>()
                        }
                    }
                }
            }
        }

        "A reset of a pending account from BEFORE email first removes its sign-up password" {
            // Activation and SSO remove it; a reset that activates must not leave it stored either.
            apiApp {
                anonymous {
                    val email = freshEmail("legacy-reset")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id
                    seedLegacyPassword(id)

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
                            provider = provider, token = resetToken, password = "Victim-chose-1!",
                        ),
                    ) { status shouldBe HttpStatusCode.OK }

                    isPending(id) shouldBe false
                    records.findAllByOwner(AuthRecord.Password, realm.id, UserId(id)).size shouldBe 1
                    passwordSignIn(email) { response -> response.status shouldBe HttpStatusCode.Forbidden }
                }
            }
        }

        //  Signing up again: growing delays, silently  ///////////////////////////////////////////////////////////

        "Signing up again re-sends the link under GROWING delays per account — and says nothing about them" {
            apiApp {
                anonymous {
                    val email = freshEmail("growing")
                    signUp(email)

                    val delays = config.activationResendDelays

                    withClue("right after sign-up: too soon, nothing goes out") {
                        signUp(email)
                        emails.capturedTo(email) shouldBe emptyList()
                    }

                    // Walk the first three delays: each re-send is withheld one minute early and sent once due.
                    delays.take(3).forEachIndexed { index, delay ->
                        advance(delay - 1.minutes)

                        withClue("one minute before delay #${index + 1} (${delay}) has passed") {
                            signUp(email)
                            emails.capturedTo(email) shouldBe emptyList()
                        }

                        advance(1.minutes)

                        withClue("once delay #${index + 1} (${delay}) has passed") {
                            signUp(email)
                            emails.capturedTo(email).size shouldBe 1
                        }
                    }

                    withClue("still the one account") {
                        users.byEmail(EmailAddress.of(email)).shouldNotBeNull()
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

                    withClue("precondition: the name carries the address's local part until retirement") {
                        userOf(email).shouldNotBeNull().value.name shouldBe email.substringBefore("@")
                    }

                    withClue("one minute before expiry the sweep leaves it alone") {
                        advance(config.pendingAccountLifetime - 1.minutes)
                        runExpiryWorker()
                        userOf(email).shouldNotBeNull()._id shouldBe id
                    }

                    advance(2.minutes)
                    runExpiryWorker()

                    val retired = users.byId(id).shouldNotBeNull()

                    withClue("the row stays — other collections may reference it — under a retired address") {
                        retired.value.email.value shouldStartWith AccountRetirement.retiredKeyPrefix(EmailAddress.of(email))
                        retired.value.email.value shouldEndWith ".invalid"
                        AccountRetirement.isRetired(retired.value.email) shouldBe true
                        retired.value.email.isValidFormat shouldBe false
                    }

                    withClue("NO plaintext of the address stays on the row — not in the key, not in the name") {
                        val localPart = email.substringBefore("@")
                        retired.value.email.value shouldNotContain localPart
                        retired.value.name shouldNotContain localPart
                    }

                    withClue("its password, tokens and marker are gone") {
                        hasPassword(id) shouldBe false
                        isPending(id) shouldBe false
                    }

                    withClue("the address is free, and the retire itself DELETED every auth row (not merely let it expire)") {
                        userOf(email).shouldBeNull()
                        retired.value.email shouldBe AccountRetirement.of(UserId(id)).rename(EmailAddress.of(email))
                        rawRowsOf(id) shouldBe emptyMap()
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

                    withClue("before expiry, signing up again keeps the SAME pending account") {
                        advance(config.pendingAccountLifetime - 1.minutes)
                        signUp(email)
                        userOf(email).shouldNotBeNull()._id shouldBe oldId
                    }

                    advance(2.minutes)
                    signUp(email)

                    val newUser = userOf(email).shouldNotBeNull()
                    newUser._id shouldNotBe oldId
                    AccountRetirement.isRetired(users.byId(oldId).shouldNotBeNull().value.email) shouldBe true
                }
            }
        }

        "A link re-sent just before expiry dies WITH the account" {
            apiApp {
                anonymous {
                    val email = freshEmail("late-link")
                    signUp(email)
                    val oldId = userOf(email).shouldNotBeNull()._id

                    advance(config.pendingAccountLifetime - 1.hours)
                    signUp(email)
                    val lateToken = activationTokenTo(email)

                    // Well inside the link's own 24 h, but past the account's 7 days.
                    advance(2.hours)

                    activate(lateToken) shouldBe false
                    withClue("and signing up again now starts a FRESH account, not the expired one") {
                        signUp(email)
                        userOf(email).shouldNotBeNull()._id shouldNotBe oldId
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
                    val activationToken = activationTokenTo(email)
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

                    withClue("the activation link left over from the sign-up sets NO password on the now active account") {
                        activate(activationToken) shouldBe false
                        passwordSignIn(email) { response -> response.status shouldBe HttpStatusCode.Forbidden }
                    }

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

        "The activation page's own cancel button: the ACTIVATION token cancels too" {
            apiApp {
                anonymous {
                    val email = freshEmail("cancel-by-activation-token")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    cancel(activationTokenTo(email)) shouldBe true

                    AccountRetirement.isRetired(users.byId(id).shouldNotBeNull().value.email) shouldBe true
                    rawRowsOf(id) shouldBe emptyMap()
                }
            }
        }

        //  Retire vs activation: CLAIM FIRST  //////////////////////////////////////////////////////////////////

        "Activation wins (it commits before the retire claims): NOTHING of the account is touched" {
            apiApp {
                anonymous {
                    val email = freshEmail("activation-wins")
                    signUp(email)
                    val cancelToken = cancelTokenTo(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    activate(activationTokenTo(email)) shouldBe true
                    cancel(cancelToken) shouldBe false

                    withClue("credentials still there, address NOT renamed, and the account signs in") {
                        hasPassword(id) shouldBe true
                        users.byId(id).shouldNotBeNull().value.email shouldBe EmailAddress.of(email)
                        passwordSignIn(email) { response ->
                            response.apiResponseData<AuthSignInResponse>().shouldBeInstanceOf<AuthSignInResponse.Success>()
                        }
                    }
                }
            }
        }

        "Retire wins (it claimed first): an activation landing mid-retire changes nothing, and the retire completes" {
            apiApp {
                anonymous {
                    val email = freshEmail("retire-wins")
                    signUp(email)
                    val activationToken = activationTokenTo(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    // The activation runs in the window right after the retire's claim, while its token and the
                    // marker still exist — exactly the request that read the marker before the claim and commits after.
                    var midRetireActivation: Boolean? = null
                    RetireGate.afterRetireClaim = {
                        midRetireActivation = authSystem.activate(
                            ActivationHardeningRealm.REALM,
                            AuthActivateAccountRequest(provider = provider, token = activationToken, password = password),
                        ).success
                    }

                    cancel(cancelTokenTo(email)) shouldBe true
                    RetireGate.afterRetireClaim = {}

                    midRetireActivation shouldBe false

                    withClue("fully retired: renamed, every auth row gone, and the activation link cannot revive it") {
                        users.byId(id).shouldNotBeNull().value.email shouldBe
                                AccountRetirement.of(UserId(id)).rename(EmailAddress.of(email))
                        rawRowsOf(id) shouldBe emptyMap()
                        activate(activationToken) shouldBe false
                    }
                }
            }
        }

        "A retire that stops part-way stays claimed and blocked, and the next sweep finishes it" {
            apiApp {
                anonymous {
                    val email = freshEmail("partial-retire")
                    signUp(email)
                    val activationToken = activationTokenTo(email)
                    val id = userOf(email).shouldNotBeNull()._id

                    RetireGate.beforeRename = { error("the user store is down") }
                    cancel(cancelTokenTo(email)) shouldBe false
                    RetireGate.beforeRename = {}

                    withClue("claimed but unfinished: credentials gone, address still held, NOTHING can activate it") {
                        records.findLatestRecordBy(AuthRecord.PendingActivation, realm.id, UserId(id)).shouldNotBeNull()
                            .value.claimedBy shouldBe AuthRecord.PendingActivation.CLAIMED_BY_RETIRE
                        hasPassword(id) shouldBe false
                        users.byId(id).shouldNotBeNull().value.email shouldBe EmailAddress.of(email)
                        activate(activationToken) shouldBe false
                        githubSignIn(FakeGithub.verified(email)) { response ->
                            response.status shouldBe HttpStatusCode.Forbidden
                        }
                    }

                    // The account is FRESH (day 0): only the claim makes the sweep take it.
                    runExpiryWorker()

                    users.byId(id).shouldNotBeNull().value.email shouldBe
                            AccountRetirement.of(UserId(id)).rename(EmailAddress.of(email))
                    rawRowsOf(id) shouldBe emptyMap()
                    withClue("the address is free again") { signUp(email) }
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

        "SSO with a VERIFIED address activates a pending account from before email first and DELETES its password" {
            // The pre-registration takeover: an attacker signed up the victim's address with a password they
            // know, before email first; the victim later signs in with GitHub. The attacker's password must
            // never work — which is why SSO keeps deleting passwords (decision 1 of ULTRA-00231).
            apiApp {
                anonymous {
                    val email = freshEmail("victim")
                    signUp(email)
                    val id = userOf(email).shouldNotBeNull()._id
                    seedLegacyPassword(id)

                    githubSignIn(FakeGithub.verified(email)) { response ->
                        response.status shouldBe HttpStatusCode.OK
                        response.apiResponseData<AuthSignInResponse>().shouldBeInstanceOf<AuthSignInResponse.Success>()
                    }

                    isPending(id) shouldBe false
                    hasPassword(id) shouldBe false

                    withClue("the pre-registered password is dead: a plain 403") {
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

                    seedLegacyPassword(id)

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

        //  SSO on an active account, or a new one (ULTRA-00118)  ///////////////////////////////////////////////

        "SSO with an UNVERIFIED address never reaches an ACTIVE account; a VERIFIED one signs in" {
            // The takeover: a provider account that claims the victim's address without verifying it
            apiApp {
                anonymous {
                    val email = freshEmail("active-victim")
                    signUp(email)
                    activate(activationTokenTo(email)) shouldBe true
                    val id = userOf(email).shouldNotBeNull()._id

                    githubSignIn(FakeGithub.unverified(email)) { response ->
                        response.status shouldBe HttpStatusCode.Forbidden
                        response.apiResponseData<AuthSignInResponse>().shouldBeNull()
                    }

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.OAuth(provider = GithubSsoAuth.ID, token = FakeGithub.unverified(email)),
                    ) {
                        status shouldBe HttpStatusCode.BadRequest
                        apiResponseData<AuthSignUpResponse>()?.signIn.shouldBeNull()
                    }

                    githubSignIn(FakeGithub.verified(email)) { response ->
                        response.status shouldBe HttpStatusCode.OK
                        response.apiResponseData<AuthSignInResponse>().shouldBeInstanceOf<AuthSignInResponse.Success>()
                    }

                    withClue("the account and its password are untouched") {
                        userOf(email).shouldNotBeNull()._id shouldBe id
                        hasPassword(id) shouldBe true
                    }
                }
            }
        }

        "SSO sign-UP with an UNVERIFIED address creates no account" {
            apiApp {
                anonymous {
                    val email = freshEmail("sso-new")

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.OAuth(provider = GithubSsoAuth.ID, token = FakeGithub.unverified(email)),
                    ) {
                        status shouldBe HttpStatusCode.BadRequest
                    }

                    userOf(email).shouldBeNull()

                    api.authLogin.signUp(
                        realmParam,
                        body = AuthSignUpRequest.OAuth(provider = GithubSsoAuth.ID, token = FakeGithub.verified(email)),
                    ) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<AuthSignUpResponse>().shouldNotBeNull().signIn.shouldNotBeNull()
                    }

                    userOf(email).shouldNotBeNull()
                }
            }
        }
    }
}
