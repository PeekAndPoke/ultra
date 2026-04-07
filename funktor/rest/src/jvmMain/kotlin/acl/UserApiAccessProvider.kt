package io.peekandpoke.funktor.rest.acl

import io.peekandpoke.ultra.security.user.User

/**
 * Provides a [UserApiAccessMatrix] evaluated for a specific [User].
 *
 * Implementations iterate all registered API routes and call `estimateAccess(user)` on each.
 */
interface UserApiAccessProvider {
    fun describeForUser(user: User): UserApiAccessMatrix
}
