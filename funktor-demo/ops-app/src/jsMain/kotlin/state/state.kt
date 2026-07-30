package io.peekandpoke.funktor.demo.opsapp.state

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.common.OperatorUserModel

class OpsAppState(
    val auth: AuthState<OperatorUserModel>,
)
