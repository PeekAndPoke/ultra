@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.Aliased
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

enum class AqlBooleanOperator(val op: String) {
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

enum class AqlArrayOperator(val op: String) {
    ANY("ANY"),
    NONE("NONE"),
    ALL("ALL"),
}

enum class AqlLogicOperator(val op: String) {
    AND("AND"),
    OR("OR"),
}

typealias AqlPartialBooleanExpression<T> = (AqlExpression<T>) -> AqlExpression<Boolean>

data class AqlArrayOpExpr<T>(
    val expression: AqlExpression<out Collection<T>>,
    val op: AqlArrayOperator,
    private val type: TypeRef<T>,
) : AqlExpression<T>, Aliased {

    override fun getAlias() = if (expression is Aliased) expression.getAlias() + "_${op.op}" else "v"

    override fun getType() = type

    override fun print(p: AqlPrinter) {
        p.append(expression).append(" ${op.op}")
    }
}

@VaultFunctionMarker
inline infix fun <reified T> AqlExpression<out Collection<T>>.ANY(
    partial: AqlPartialBooleanExpression<T>,
): AqlExpression<Boolean> =
    partial(AqlArrayOpExpr(this, AqlArrayOperator.ANY, kType()))

// TODO: Write unit tests
@VaultFunctionMarker
inline infix fun <reified T> AqlExpression<out Collection<T>>.ANY_IN(
    other: AqlExpression<out Collection<T>>,
): AqlExpression<Boolean> =
    this ANY { it IN other }

@VaultFunctionMarker
inline infix fun <reified T> AqlExpression<out Collection<T>>.NONE(
    partial: AqlPartialBooleanExpression<T>,
): AqlExpression<Boolean> =
    partial(AqlArrayOpExpr(this, AqlArrayOperator.NONE, kType()))

// TODO: Write unit tests
@VaultFunctionMarker
inline infix fun <reified T> AqlExpression<out Collection<T>>.NONE_IN(
    other: AqlExpression<out Collection<T>>,
): AqlExpression<Boolean> =
    this NONE { it IN other }

@VaultFunctionMarker
inline infix fun <reified T> AqlExpression<out Collection<T>>.ALL(
    partial: AqlPartialBooleanExpression<T>,
): AqlExpression<Boolean> =
    partial(AqlArrayOpExpr(this, AqlArrayOperator.ALL, kType()))

// TODO: Write unit tests
@VaultFunctionMarker
inline infix fun <reified T> AqlExpression<out Collection<T>>.ALL_IN(
    other: AqlExpression<out Collection<T>>,
): AqlExpression<Boolean> =
    this ALL { it IN other }

@VaultFunctionMarker
fun <T> EQ(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x EQ value }

@VaultFunctionMarker
fun <T> EQ(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x EQ value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.EQ(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.EQ, right = value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.EQ(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.EQ, right = value)

@VaultFunctionMarker
fun <T> NE(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x NE value }

@VaultFunctionMarker
fun <T> NE(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x NE value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.NE(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.NE, right = value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.NE(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.NE, right = value)

@VaultFunctionMarker
fun <T> GT(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x GT value }

@VaultFunctionMarker
fun <T> GT(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x GT value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.GT(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(this, AqlBooleanOperator.GT, value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.GT(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.GT, right = value)

@VaultFunctionMarker
fun <T> GTE(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x GTE value }

@VaultFunctionMarker
fun <T> GTE(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x GTE value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.GTE(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.GTE, right = value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.GTE(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.GTE, right = value)

@VaultFunctionMarker
fun <T> LT(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x LT value }

@VaultFunctionMarker
fun <T> LT(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x LT value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.LT(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.LT, right = value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.LT(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LT, right = value)

@VaultFunctionMarker
fun <T> LTE(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x LTE value }

@VaultFunctionMarker
fun <T> LTE(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x LTE value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.LTE(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.LTE, right = value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.LTE(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LTE, right = value)

@VaultFunctionMarker
fun <T> IN(value: Array<T>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

@VaultFunctionMarker
fun <T> IN(value: Collection<T>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

@JvmName("Partial_IN_List_Expression")
@VaultFunctionMarker
fun <T> IN(value: AqlExpression<List<T>>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

@JvmName("Partial_IN_Set_Expression")
@VaultFunctionMarker
fun <T> IN(value: AqlExpression<Set<T>>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.IN(value: Array<T>): AqlExpression<Boolean> =
    IN(value.toList())

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.IN(value: Collection<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.IN, right = value.aql)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.IN(value: AqlExpression<out Collection<T>>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.IN, right = value)

@VaultFunctionMarker
fun <T> NOT_IN(value: Array<T>): AqlPartialBooleanExpression<T> =
    { x -> x NOT_IN value }

@VaultFunctionMarker
fun <T> NOT_IN(value: Collection<T>): AqlPartialBooleanExpression<T> =
    { x -> x NOT_IN value }

@VaultFunctionMarker
fun <T> NOT_IN(value: AqlExpression<List<T>>): AqlPartialBooleanExpression<T> =
    { x -> x NOT_IN value }

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.NOT_IN(value: Array<T>): AqlExpression<Boolean> =
    NOT_IN(value.toList())

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.NOT_IN(value: Collection<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.NOT_IN, right = value.aql)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.NOT_IN(value: AqlExpression<List<T>>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.NOT_IN, right = value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.LIKE(value: String): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LIKE, right = value.aql)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.LIKE(value: AqlExpression<String>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LIKE, right = value)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.REGEX(value: String): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.REGEX, value.aql)

@VaultFunctionMarker
infix fun <T> AqlExpression<T>.REGEX(value: AqlExpression<String>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.REGEX, right = value)

@VaultFunctionMarker
infix fun AqlExpression<Boolean>.AND(value: Boolean): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.AND, right = value.aql)

@VaultFunctionMarker
infix fun AqlExpression<Boolean>.AND(value: AqlExpression<Boolean>): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.AND, right = value)

@VaultFunctionMarker
infix fun AqlExpression<Boolean>.OR(value: Boolean): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.OR, right = value.aql)

@VaultFunctionMarker
infix fun AqlExpression<Boolean>.OR(value: AqlExpression<Boolean>): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.OR, right = value)

@VaultFunctionMarker
@JvmName("NOT_1")
fun AqlExpression<Boolean>.NOT(): AqlExpression<Boolean> =
    AqlFunc.NOT.boolCall(this)

@VaultFunctionMarker
@JvmName("NOT_2")
fun NOT(expr: AqlExpression<Boolean>): AqlExpression<Boolean> =
    AqlFunc.NOT.boolCall(expr)
