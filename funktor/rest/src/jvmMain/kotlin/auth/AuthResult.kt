package io.peekandpoke.funktor.rest.auth

/** Result of checking authorization rules; [isSuccess] is true when no rules failed. */
data class AuthResult<PARAMS, BODY>(
    val failedRules: List<AuthRule<PARAMS, BODY>>,
) {
    fun isSuccess() = failedRules.isEmpty()
}
