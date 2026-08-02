package io.peekandpoke.funktor

import io.peekandpoke.funktor.messaging.senders.CapturedEmails
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import kotlinx.coroutines.runBlocking

abstract class FunktorApiSpec : AppSpec<FunktorAllTestConfig>(testApp) {

    protected val realm by service(TestUserRealm::class)
    protected val usersRepo by service(TestUsersRepo::class)

    /**
     * Every mail the app sends during a test.
     *
     * Nothing wires this — `funktorMessaging` captures instead of sending whenever the app runs in
     * test mode. One instance per spec, so it accumulates across the tests within one; call `clear()`
     * at the start of whatever you are about to assert on.
     */
    protected val capturedEmails by service(CapturedEmails::class)

    /** The persisted copies of those mails — what `EmailStoring` actually wrote to the database. */
    protected val sentMessages by service(SentMessagesStorage::class)

    private val superUser by lazy {
        runBlocking {
            usersRepo.insert(
                TestUser(
                    name = "Super User",
                    email = EmailAddress.of("super-${this@FunktorApiSpec::class.simpleName}@test.com"),
                    isSuperUser = true,
                )
            )
        }
    }

    /** Exposed so a refresh can be asserted to return a token for the SAME user. */
    protected val superUserId: UserId by lazy { UserId(superUser._id) }

    protected val superUserToken: String by lazy {
        runBlocking { realm.generateJwt(superUser, selectedOrg = null) }
    }

    private val regularUser by lazy {
        runBlocking {
            usersRepo.insert(
                TestUser(
                    name = "Regular User",
                    email = EmailAddress.of("regular-${this@FunktorApiSpec::class.simpleName}@test.com"),
                    isSuperUser = false,
                )
            )
        }
    }

    /** Exposed so a refresh can be asserted to return a token for the SAME user. */
    protected val regularUserId: UserId by lazy { UserId(regularUser._id) }

    protected val regularUserToken: String by lazy {
        runBlocking { realm.generateJwt(regularUser, selectedOrg = null) }
    }
}
