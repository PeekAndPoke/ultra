@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.lang.Aliased

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

inline infix fun <reified T> AqlExpression<out Collection<T>>.ANY(
    partial: AqlPartialBooleanExpression<T>,
): AqlExpression<Boolean> =
    partial(AqlArrayOpExpr(this, AqlArrayOperator.ANY, kType()))

inline infix fun <reified T> AqlExpression<out Collection<T>>.ANY_IN(
    other: AqlExpression<out Collection<T>>,
): AqlExpression<Boolean> =
    this ANY { it IN other }

inline infix fun <reified T> AqlExpression<out Collection<T>>.NONE(
    partial: AqlPartialBooleanExpression<T>,
): AqlExpression<Boolean> =
    partial(AqlArrayOpExpr(this, AqlArrayOperator.NONE, kType()))

inline infix fun <reified T> AqlExpression<out Collection<T>>.NONE_IN(
    other: AqlExpression<out Collection<T>>,
): AqlExpression<Boolean> =
    this NONE { it IN other }

inline infix fun <reified T> AqlExpression<out Collection<T>>.ALL(
    partial: AqlPartialBooleanExpression<T>,
): AqlExpression<Boolean> =
    partial(AqlArrayOpExpr(this, AqlArrayOperator.ALL, kType()))

inline infix fun <reified T> AqlExpression<out Collection<T>>.ALL_IN(
    other: AqlExpression<out Collection<T>>,
): AqlExpression<Boolean> =
    this ALL { it IN other }

fun <T> EQ(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x EQ value }

fun <T> EQ(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x EQ value }

infix fun <T> AqlExpression<T>.EQ(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.EQ, right = value)

infix fun <T> AqlExpression<T>.EQ(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.EQ, right = value)

fun <T> NE(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x NE value }

fun <T> NE(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x NE value }

infix fun <T> AqlExpression<T>.NE(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.NE, right = value)

infix fun <T> AqlExpression<T>.NE(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.NE, right = value)

fun <T> GT(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x GT value }

fun <T> GT(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x GT value }

infix fun <T> AqlExpression<T>.GT(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(this, AqlBooleanOperator.GT, value)

infix fun <T> AqlExpression<T>.GT(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.GT, right = value)

fun <T> GTE(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x GTE value }

fun <T> GTE(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x GTE value }

infix fun <T> AqlExpression<T>.GTE(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.GTE, right = value)

infix fun <T> AqlExpression<T>.GTE(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.GTE, right = value)

fun <T> LT(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x LT value }

fun <T> LT(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x LT value }

infix fun <T> AqlExpression<T>.LT(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.LT, right = value)

infix fun <T> AqlExpression<T>.LT(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LT, right = value)

fun <T> LTE(value: T?): AqlPartialBooleanExpression<T> =
    { x -> x LTE value }

fun <T> LTE(value: AqlExpression<T>): AqlPartialBooleanExpression<T> =
    { x -> x LTE value }

infix fun <T> AqlExpression<T>.LTE(value: T?): AqlExpression<Boolean> =
    AqlFilterByExpression.value(left = this, op = AqlBooleanOperator.LTE, right = value)

infix fun <T> AqlExpression<T>.LTE(value: AqlExpression<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LTE, right = value)

fun <T> IN(value: Array<T>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

fun <T> IN(value: Collection<T>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

@JvmName("Partial_IN_List_Expression")
fun <T> IN(value: AqlExpression<List<T>>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

@JvmName("Partial_IN_Set_Expression")
fun <T> IN(value: AqlExpression<Set<T>>): AqlPartialBooleanExpression<T> =
    { x -> x IN value }

infix fun <T> AqlExpression<T>.IN(value: Array<T>): AqlExpression<Boolean> =
    IN(value.toList())

infix fun <T> AqlExpression<T>.IN(value: Collection<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.IN, right = value.aql)

infix fun <T> AqlExpression<T>.IN(value: AqlExpression<out Collection<T>>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.IN, right = value)

fun <T> NOT_IN(value: Array<T>): AqlPartialBooleanExpression<T> =
    { x -> x NOT_IN value }

fun <T> NOT_IN(value: Collection<T>): AqlPartialBooleanExpression<T> =
    { x -> x NOT_IN value }

fun <T> NOT_IN(value: AqlExpression<List<T>>): AqlPartialBooleanExpression<T> =
    { x -> x NOT_IN value }

infix fun <T> AqlExpression<T>.NOT_IN(value: Array<T>): AqlExpression<Boolean> =
    NOT_IN(value.toList())

infix fun <T> AqlExpression<T>.NOT_IN(value: Collection<T>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.NOT_IN, right = value.aql)

infix fun <T> AqlExpression<T>.NOT_IN(value: AqlExpression<List<T>>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.NOT_IN, right = value)

infix fun <T> AqlExpression<T>.LIKE(value: String): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LIKE, right = value.aql)

infix fun <T> AqlExpression<T>.LIKE(value: AqlExpression<String>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.LIKE, right = value)

infix fun <T> AqlExpression<T>.REGEX(value: String): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.REGEX, value.aql)

infix fun <T> AqlExpression<T>.REGEX(value: AqlExpression<String>): AqlExpression<Boolean> =
    AqlFilterByExpression.expr(left = this, op = AqlBooleanOperator.REGEX, right = value)

infix fun AqlExpression<Boolean>.AND(value: Boolean): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.AND, right = value.aql)

infix fun AqlExpression<Boolean>.AND(value: AqlExpression<Boolean>): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.AND, right = value)

infix fun AqlExpression<Boolean>.OR(value: Boolean): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.OR, right = value.aql)

infix fun AqlExpression<Boolean>.OR(value: AqlExpression<Boolean>): AqlExpression<Boolean> =
    AqlFilterLogicExpression(left = this, op = AqlLogicOperator.OR, right = value)
