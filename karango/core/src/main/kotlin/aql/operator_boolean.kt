@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.Aliased
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

enum class BooleanOperator(val op: String) {
    EQ("=="),
    NE("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    IN("IN"),
    NOT_IN("NOT IN"),
    LIKE("LIKE"),
    REGEX("=~"),
}

enum class ArrayOperator(val op: String) {
    ANY("ANY"),
    NONE("NONE"),
    ALL("ALL"),
}

enum class LogicOperator(val op: String) {
    AND("AND"),
    OR("OR"),
}

typealias PartialBooleanExpression<T> = (Expression<T>) -> Expression<Boolean>

data class ArrayOpExpr<T>(
    val expression: Expression<out Collection<T>>,
    val op: ArrayOperator,
    private val type: TypeRef<T>,
) : Expression<T>, Aliased {

    override fun getAlias() = if (expression is Aliased) expression.getAlias() + "_${op.op}" else "v"

    override fun getType() = type

    override fun print(p: Printer) = p.append(expression).append(" ${op.op}")
}

@VaultFunctionMarker
inline infix fun <reified T> Expression<out Collection<T>>.ANY(partial: PartialBooleanExpression<T>): Expression<Boolean> =
    partial(ArrayOpExpr(this, ArrayOperator.ANY, kType()))

// TODO: Write unit tests
@VaultFunctionMarker
inline infix fun <reified T> Expression<out Collection<T>>.ANY_IN(other: Expression<out Collection<T>>): Expression<Boolean> =
    this ANY { it IN other }

@VaultFunctionMarker
inline infix fun <reified T> Expression<out Collection<T>>.NONE(partial: PartialBooleanExpression<T>): Expression<Boolean> =
    partial(ArrayOpExpr(this, ArrayOperator.NONE, kType()))

// TODO: Write unit tests
@VaultFunctionMarker
inline infix fun <reified T> Expression<out Collection<T>>.NONE_IN(other: Expression<out Collection<T>>): Expression<Boolean> =
    this NONE { it IN other }

@VaultFunctionMarker
inline infix fun <reified T> Expression<out Collection<T>>.ALL(partial: PartialBooleanExpression<T>): Expression<Boolean> =
    partial(ArrayOpExpr(this, ArrayOperator.ALL, kType()))

// TODO: Write unit tests
@VaultFunctionMarker
inline infix fun <reified T> Expression<out Collection<T>>.ALL_IN(other: Expression<out Collection<T>>): Expression<Boolean> =
    this ALL { it IN other }

@VaultFunctionMarker
fun <T> EQ(value: T?): PartialBooleanExpression<T> = { x -> x EQ value }

@VaultFunctionMarker
fun <T> EQ(value: Expression<T>): PartialBooleanExpression<T> = { x -> x EQ value }

@VaultFunctionMarker
infix fun <T> Expression<T>.EQ(value: T?): Expression<Boolean> = FilterBy.value(this, BooleanOperator.EQ, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.EQ(value: Expression<T>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.EQ, value)

@VaultFunctionMarker
fun <T> NE(value: T?): PartialBooleanExpression<T> = { x -> x NE value }

@VaultFunctionMarker
fun <T> NE(value: Expression<T>): PartialBooleanExpression<T> = { x -> x NE value }

@VaultFunctionMarker
infix fun <T> Expression<T>.NE(value: T?): Expression<Boolean> = FilterBy.value(this, BooleanOperator.NE, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.NE(value: Expression<T>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.NE, value)

@VaultFunctionMarker
fun <T> GT(value: T?): PartialBooleanExpression<T> = { x -> x GT value }

@VaultFunctionMarker
fun <T> GT(value: Expression<T>): PartialBooleanExpression<T> = { x -> x GT value }

@VaultFunctionMarker
infix fun <T> Expression<T>.GT(value: T?): Expression<Boolean> = FilterBy.value(this, BooleanOperator.GT, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.GT(value: Expression<T>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.GT, value)

@VaultFunctionMarker
fun <T> GTE(value: T?): PartialBooleanExpression<T> = { x -> x GTE value }

@VaultFunctionMarker
fun <T> GTE(value: Expression<T>): PartialBooleanExpression<T> = { x -> x GTE value }

@VaultFunctionMarker
infix fun <T> Expression<T>.GTE(value: T?): Expression<Boolean> = FilterBy.value(this, BooleanOperator.GTE, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.GTE(value: Expression<T>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.GTE, value)

@VaultFunctionMarker
fun <T> LT(value: T?): PartialBooleanExpression<T> = { x -> x LT value }

@VaultFunctionMarker
fun <T> LT(value: Expression<T>): PartialBooleanExpression<T> = { x -> x LT value }

@VaultFunctionMarker
infix fun <T> Expression<T>.LT(value: T?): Expression<Boolean> = FilterBy.value(this, BooleanOperator.LT, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.LT(value: Expression<T>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.LT, value)

@VaultFunctionMarker
fun <T> LTE(value: T?): PartialBooleanExpression<T> = { x -> x LTE value }

@VaultFunctionMarker
fun <T> LTE(value: Expression<T>): PartialBooleanExpression<T> = { x -> x LTE value }

@VaultFunctionMarker
infix fun <T> Expression<T>.LTE(value: T?): Expression<Boolean> = FilterBy.value(this, BooleanOperator.LTE, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.LTE(value: Expression<T>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.LTE, value)

@VaultFunctionMarker
fun <T> IN(value: Array<T>): PartialBooleanExpression<T> = { x -> x IN value }

@VaultFunctionMarker
fun <T> IN(value: Collection<T>): PartialBooleanExpression<T> = { x -> x IN value }

@JvmName("Partial_IN_List_Expression")
@VaultFunctionMarker
fun <T> IN(value: Expression<List<T>>): PartialBooleanExpression<T> = { x -> x IN value }

@JvmName("Partial_IN_Set_Expression")
@VaultFunctionMarker
fun <T> IN(value: Expression<Set<T>>): PartialBooleanExpression<T> = { x -> x IN value }

@VaultFunctionMarker
infix fun <T> Expression<T>.IN(value: Array<T>): Expression<Boolean> = IN(value.toList())

@VaultFunctionMarker
infix fun <T> Expression<T>.IN(value: Collection<T>): Expression<Boolean> =
    FilterBy.value(this, BooleanOperator.IN, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.IN(value: Expression<out Collection<T>>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.IN, value)

@VaultFunctionMarker
fun <T> NOT_IN(value: Array<T>): PartialBooleanExpression<T> = { x -> x NOT_IN value }

@VaultFunctionMarker
fun <T> NOT_IN(value: Collection<T>): PartialBooleanExpression<T> = { x -> x NOT_IN value }

@VaultFunctionMarker
fun <T> NOT_IN(value: Expression<List<T>>): PartialBooleanExpression<T> = { x -> x NOT_IN value }

@VaultFunctionMarker
infix fun <T> Expression<T>.NOT_IN(value: Array<T>): Expression<Boolean> = NOT_IN(value.toList())

@VaultFunctionMarker
infix fun <T> Expression<T>.NOT_IN(value: Collection<T>): Expression<Boolean> =
    FilterBy.value(this, BooleanOperator.NOT_IN, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.NOT_IN(value: Expression<List<T>>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.NOT_IN, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.LIKE(value: String): Expression<Boolean> = FilterBy.value(this, BooleanOperator.LIKE, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.LIKE(value: Expression<String>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.LIKE, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.REGEX(value: String): Expression<Boolean> =
    FilterBy.value(this, BooleanOperator.REGEX, value)

@VaultFunctionMarker
infix fun <T> Expression<T>.REGEX(value: Expression<String>): Expression<Boolean> =
    FilterBy.expr(this, BooleanOperator.REGEX, value)

@VaultFunctionMarker
infix fun Expression<Boolean>.AND(value: Boolean): Expression<Boolean> = FilterLogic(this, LogicOperator.AND, value.aql)

@VaultFunctionMarker
infix fun Expression<Boolean>.AND(value: Expression<Boolean>): Expression<Boolean> =
    FilterLogic(this, LogicOperator.AND, value)

@VaultFunctionMarker
infix fun Expression<Boolean>.OR(value: Boolean): Expression<Boolean> = FilterLogic(this, LogicOperator.OR, value.aql)

@VaultFunctionMarker
infix fun Expression<Boolean>.OR(value: Expression<Boolean>): Expression<Boolean> =
    FilterLogic(this, LogicOperator.OR, value)

@VaultFunctionMarker
@JvmName("NOT_1")
fun Expression<Boolean>.NOT(): Expression<Boolean> = AqlFunc.NOT.boolCall(this)

@VaultFunctionMarker
@JvmName("NOT_2")
fun NOT(expr: Expression<Boolean>): Expression<Boolean> = AqlFunc.NOT.boolCall(expr)
