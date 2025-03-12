package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Expression

fun anyOf(vararg exp: Expression<Boolean>) = exp.toList().any

val Collection<Expression<Boolean>>.any
    get() = when {
        isEmpty() -> true.aql
        else -> reduce { acc, next -> acc OR next }
    }

val Collection<Expression<Boolean>>.all
    get() = when {
        isEmpty() -> false.aql
        else -> reduce { acc, next -> acc AND next }
    }
