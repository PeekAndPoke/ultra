package io.peekandpoke.funktor.demo.b2bapp.state

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.common.B2bUserModel

class B2bAppState(
    val auth: AuthState<B2bUserModel>,
)
