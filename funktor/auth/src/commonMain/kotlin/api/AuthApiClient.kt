package de.peekandpoke.funktor.auth.api

import de.peekandpoke.funktor.auth.model.AuthActivateRequest
import de.peekandpoke.funktor.auth.model.AuthActivateResponse
import de.peekandpoke.funktor.auth.model.AuthRealmModel
import de.peekandpoke.funktor.auth.model.AuthRecoveryRequest
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.call
import kotlinx.coroutines.flow.Flow

class AuthApiClient(private val realm: String, config: Config) : ApiClient(config) {

    companion object {
        const val base = "/auth"

        val GetRealm = TypedApiEndpoint.Get(
            uri = "$base/{realm}/realm",
            response = AuthRealmModel.serializer().api(),
        )

        val SignIn = TypedApiEndpoint.Post(
            uri = "$base/{realm}/signin",
            body = AuthSignInRequest.serializer(),
            response = AuthSignInResponse.serializer().api(),
        )

        val Update = TypedApiEndpoint.Put(
            uri = "$base/{realm}/update",
            body = AuthUpdateRequest.serializer(),
            response = AuthUpdateResponse.serializer().api(),
        )

        val Recover = TypedApiEndpoint.Put(
            uri = "$base/{realm}/recover",
            body = AuthRecoveryRequest.serializer(),
            response = AuthRecoveryResponse.serializer().api(),
        )

        val SignUp = TypedApiEndpoint.Post(
            uri = "$base/{realm}/signup",
            body = AuthSignUpRequest.serializer(),
            response = AuthSignUpResponse.serializer().api(),
        )

        val Activate = TypedApiEndpoint.Post(
            uri = "$base/{realm}/activate",
            body = AuthActivateRequest.serializer(),
            response = AuthActivateResponse.serializer().api(),
        )
    }

    fun getRealm(): Flow<ApiResponse<AuthRealmModel>> = call(
        GetRealm(
            "realm" to realm,
        )
    )

    fun signIn(request: AuthSignInRequest): Flow<ApiResponse<AuthSignInResponse>> = call(
        SignIn(
            "realm" to realm,
            body = request,
        )
    )

    fun update(request: AuthUpdateRequest): Flow<ApiResponse<AuthUpdateResponse>> = call(
        Update(
            "realm" to realm,
            body = request,
        )
    )

    fun recover(request: AuthRecoveryRequest): Flow<ApiResponse<AuthRecoveryResponse>> = call(
        Recover(
            "realm" to realm,
            body = request,
        )
    )

    fun signUp(request: AuthSignUpRequest): Flow<ApiResponse<AuthSignUpResponse>> = call(
        SignUp(
            "realm" to realm,
            body = request,
        )
    )

    fun activate(request: AuthActivateRequest): Flow<ApiResponse<AuthActivateResponse>> = call(
        Activate(
            "realm" to realm,
            body = request,
        )
    )
}
