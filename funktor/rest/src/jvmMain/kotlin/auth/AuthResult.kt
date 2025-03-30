package de.peekandpoke.ktorfx.rest.auth

data class AuthResult<PARAMS, BODY>(
    val failedRules: List<AuthRule<PARAMS, BODY>>,
) {
    fun isSuccess() = failedRules.isEmpty()
}
