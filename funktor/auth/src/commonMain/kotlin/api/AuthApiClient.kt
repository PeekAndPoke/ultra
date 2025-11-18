package de.peekandpoke.funktor.auth.api

import de.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import de.peekandpoke.funktor.auth.model.AuthActivateActivateResponse
import de.peekandpoke.funktor.auth.model.AuthRealmModel
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.call
import kotlinx.coroutines.flow.Flow

class AuthApiClient(private val realm: String, config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/auth"

        val GetRealm = TypedApiEndpoint.Get(
            uri = "$BASE/{realm}/realm",
            response = AuthRealmModel.serializer().api(),
        )

        val SignIn = TypedApiEndpoint.Post(
            uri = "$BASE/{realm}/signin",
            body = AuthSignInRequest.serializer(),
            response = AuthSignInResponse.serializer().api(),
        )

        val SignUp = TypedApiEndpoint.Post(
            uri = "$BASE/{realm}/signup",
            body = AuthSignUpRequest.serializer(),
            response = AuthSignUpResponse.serializer().api(),
        )

        val ActivateAccount = TypedApiEndpoint.Post(
            uri = "$BASE/{realm}/activate",
            body = AuthActivateAccountRequest.serializer(),
            response = AuthActivateActivateResponse.serializer().api(),
        )

        val SetPassword = TypedApiEndpoint.Put(
            uri = "$BASE/{realm}/update",
            body = AuthSetPasswordRequest.serializer(),
            response = AuthSetPasswordResponse.serializer().api(),
        )

        val RecoverAccountInitPasswordReset = TypedApiEndpoint.Put(
            uri = "$BASE/{realm}/recover/reset-password/init",
            body = AuthRecoverAccountRequest.InitPasswordReset.serializer(),
            response = AuthRecoverAccountResponse.InitPasswordReset.serializer().api(),
        )

        val RecoverAccountValidatePasswordResetToken = TypedApiEndpoint.Put(
            uri = "$BASE/{realm}/recover/password-reset/validate",
            body = AuthRecoverAccountRequest.ValidatePasswordResetToken.serializer(),
            response = AuthRecoverAccountResponse.ValidatePasswordResetToken.serializer().api(),
        )

        val RecoverAccountSetPasswordWithToken = TypedApiEndpoint.Put(
            uri = "$BASE/{realm}/recover/password-reset/set-password",
            body = AuthRecoverAccountRequest.SetPasswordWithToken.serializer(),
            response = AuthRecoverAccountResponse.SetPasswordWithToken.serializer().api(),
        )
    }

    fun getRealm(): Flow<ApiResponse<AuthRealmModel>> = call(
        GetRealm("realm" to realm)
    )

    fun signIn(request: AuthSignInRequest): Flow<ApiResponse<AuthSignInResponse>> = call(
        SignIn("realm" to realm, body = request)
    )

    fun signUp(request: AuthSignUpRequest): Flow<ApiResponse<AuthSignUpResponse>> = call(
        SignUp("realm" to realm, body = request)
    )

    fun activateAccount(request: AuthActivateAccountRequest): Flow<ApiResponse<AuthActivateActivateResponse>> = call(
        ActivateAccount("realm" to realm, body = request)
    )

    fun setPassword(request: AuthSetPasswordRequest): Flow<ApiResponse<AuthSetPasswordResponse>> = call(
        SetPassword("realm" to realm, body = request)
    )

    fun recoverAccountInitPasswordReset(
        request: AuthRecoverAccountRequest.InitPasswordReset,
    ): Flow<ApiResponse<AuthRecoverAccountResponse.InitPasswordReset>> = call(
        RecoverAccountInitPasswordReset("realm" to realm, body = request)
    )

    fun recoverAccountValidatePasswordResetToken(
        request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): Flow<ApiResponse<AuthRecoverAccountResponse.ValidatePasswordResetToken>> = call(
        RecoverAccountValidatePasswordResetToken("realm" to realm, body = request)
    )

    fun recoverAccountSetPasswordWithToken(
        request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): Flow<ApiResponse<AuthRecoverAccountResponse.SetPasswordWithToken>> = call(
        RecoverAccountSetPasswordWithToken("realm" to realm, body = request)
    )
}
