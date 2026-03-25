package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.kraft.forms.validation.given

fun PasswordPolicy.asFormRule() = given<String>(
    check = { matches(it) },
    message = { description },
)
