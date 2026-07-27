package io.peekandpoke.funktor

import io.peekandpoke.funktor.messaging.senders.CapturedEmails
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.ultra.security.user.EmailAddress
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

    protected val superUserToken: String by lazy {
        runBlocking {
            val user = usersRepo.insert(
                TestUser(
                    name = "Super User",
                    email = EmailAddress.of("super-${this@FunktorApiSpec::class.simpleName}@test.com"),
                    isSuperUser = true,
                )
            )
            realm.generateJwt(user, selectedOrg = null).token
        }
    }

    protected val regularUserToken: String by lazy {
        runBlocking {
            val user = usersRepo.insert(
                TestUser(
                    name = "Regular User",
                    email = EmailAddress.of("regular-${this@FunktorApiSpec::class.simpleName}@test.com"),
                    isSuperUser = false,
                )
            )
            realm.generateJwt(user, selectedOrg = null).token
        }
    }
}
