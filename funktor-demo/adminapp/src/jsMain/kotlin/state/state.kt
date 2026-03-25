package io.peekandpoke.funktor.demo.adminapp.state

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.common.AdminUserModel

class AdminAppState(
    val auth: AuthState<AdminUserModel>,
)
