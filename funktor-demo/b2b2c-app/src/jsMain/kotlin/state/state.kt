package io.peekandpoke.funktor.demo.b2b2capp.state

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.common.B2b2cUserModel

class B2b2cAppState(
    val auth: AuthState<B2b2cUserModel>,
)
