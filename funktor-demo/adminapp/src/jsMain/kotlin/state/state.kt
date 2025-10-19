package de.peekandpoke.funktor.demo.adminapp.state

import de.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.demo.common.AdminUserModel

class AdminAppState(
    val auth: AuthState<AdminUserModel>,
)
