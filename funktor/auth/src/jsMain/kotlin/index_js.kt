package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.PasswordPolicy
import de.peekandpoke.kraft.forms.validation.given

fun PasswordPolicy.asFormRule() = given<String>(
    check = { matches(it) },
    message = { description },
)
