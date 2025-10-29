package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType

inline fun <reified R> aqlFunc(name: String) = AqlFunctionDefinition<R>(name = name, type = kType())

class AqlFunctionDefinition<R>(private val name: String, private val type: TypeRef<R>) : AqlExpression<R> {
    fun getName(): String = name

    override fun getType(): TypeRef<R> = type

    override fun print(p: AqlPrinter) {
        p.append(name)
    }

    fun call(vararg args: AqlExpression<*>): AqlExpression<R> =
        call(type = getType(), args = args)

    @Suppress("USELESS_CAST")
    fun <X> call(type: TypeRef<X>, vararg args: AqlExpression<*>): AqlExpression<X> =
        AqlFuncCall.of(type = type, func = getName(), args = args) as AqlExpression<X>
}
