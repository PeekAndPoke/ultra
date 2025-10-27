package de.peekandpoke.karango.aql

fun anyOf(vararg exp: AqlExpression<Boolean>) = exp.toList().any

fun allOf(vararg exp: AqlExpression<Boolean>) = exp.toList().all

val Collection<AqlExpression<Boolean>>.any
    get() = when {
        isEmpty() -> false.aql
        else -> reduce { acc, next -> acc OR next }
    }

val Collection<AqlExpression<Boolean>>.anyOrTrueIfEmpty
    get() = when {
        isEmpty() -> true.aql
        else -> reduce { acc, next -> acc OR next }
    }

val Collection<AqlExpression<Boolean>>.all
    get() = when {
        isEmpty() -> true.aql
        else -> reduce { acc, next -> acc AND next }
    }

val Collection<AqlExpression<Boolean>>.allOrFalseIfEmpty
    get() = when {
        isEmpty() -> false.aql
        else -> reduce { acc, next -> acc AND next }
    }
