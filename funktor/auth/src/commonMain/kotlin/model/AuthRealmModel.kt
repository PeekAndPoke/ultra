package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRealmModel(
    val id: String,
    val providers: List<AuthProviderModel>,
    val passwordPolicy: PasswordPolicy,
) {
    val signInProviders: List<AuthProviderModel> = providers.filter {
        AuthProviderModel.Capability.SignIn in it.capabilities
    }

    val signUpProviders: List<AuthProviderModel> = providers.filter {
        AuthProviderModel.Capability.SignUp in it.capabilities
    }
}
