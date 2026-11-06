package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.model.AuthDisplayName
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.kraft.forms.validation.given

fun PasswordPolicy.asFormRule() = given<String>(
    check = { matches(it) },
    message = { refusalDescription(it) },
)

/** The activation page's display-name rule, the same one the server applies. */
fun AuthDisplayName.asFormRule() = given<String>(
    check = { refusalOf(it) == null },
    message = { refusalOf(it)?.message ?: DESCRIPTION },
)
