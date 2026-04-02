package io.peekandpoke.funktor

import io.peekandpoke.funktor.testing.AppSpec
import kotlinx.coroutines.runBlocking

abstract class FunktorApiSpec : AppSpec<FunktorAllTestConfig>(testApp) {

    protected val realm by service(TestUserRealm::class)
    protected val usersRepo by service(TestUsersRepo::class)

    protected val superUserToken: String by lazy {
        runBlocking {
            val user = usersRepo.insert(
                TestUser(
                    name = "Super User",
                    email = "super-${this@FunktorApiSpec::class.simpleName}@test.com",
                    isSuperUser = true,
                )
            )
            realm.generateJwt(user).token
        }
    }

    protected val regularUserToken: String by lazy {
        runBlocking {
            val user = usersRepo.insert(
                TestUser(
                    name = "Regular User",
                    email = "regular-${this@FunktorApiSpec::class.simpleName}@test.com",
                    isSuperUser = false,
                )
            )
            realm.generateJwt(user).token
        }
    }
}
