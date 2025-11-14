package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.PasswordPolicy
import de.peekandpoke.funktor.auth.pages.LoginPage
import de.peekandpoke.funktor.auth.pages.ResetPasswordPage
import de.peekandpoke.kraft.forms.validation.given
import de.peekandpoke.kraft.routing.RouterBuilder

fun PasswordPolicy.asFormRule() = given<String>(
    check = { matches(it) },
    message = { description },
)

fun <USER> RouterBuilder.mountAuth(
    routes: AuthFrontendRoutes = AuthFrontendRoutes(),
    state: AuthState<USER>,
) {
    mount(routes.login) { LoginPage(state = state) }
    mount(routes.resetPassword) { ResetPasswordPage(state = state, realm = it["realm"], token = it["token"]) }
}
