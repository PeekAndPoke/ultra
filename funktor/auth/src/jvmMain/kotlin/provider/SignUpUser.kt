package io.peekandpoke.funktor.auth.provider

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.ultra.vault.Stored
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AuthUserAdapter.createForSignup] for every sign-up (email and SSO): a failure to create, such as a concurrent
 * sign-up of the same address hitting the unique index, is a refused sign-up, not a 500 (ULTRA-00288). `Throwable`,
 * because a Karango user repository's failures are no `Exception`s. The adapter's own [AuthError] (a realm without
 * sign-up throws `AuthError.notSupported()`) passes through as it is.
 */
internal suspend fun <USER : AuthUser> AuthUserAdapter<USER>.createForSignupOrRefuse(
    params: AuthUserAdapter.CreateUserForSignupParams,
): Stored<USER> = try {
    createForSignup(params)
} catch (e: CancellationException) {
    throw e
} catch (e: Error) {
    throw e
} catch (e: AuthError) {
    throw e
} catch (e: Throwable) {
    throw AuthError("User already exists", cause = e)
}
