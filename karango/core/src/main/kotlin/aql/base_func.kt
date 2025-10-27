package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef

abstract class AqlFunctionExpr<R>(private val name: String, private val type: TypeRef<R>) : AqlExpression<R> {
    override fun getType(): TypeRef<R> = type

    override fun print(p: AqlPrinter) {
        p.append(name)
    }
}

abstract class AqlFunctionExpr0<R>(name: String, type: TypeRef<R>) : AqlFunctionExpr<R>(name, type)

abstract class AqlFunctionExpr1<P1, R>(name: String, type: TypeRef<R>) : AqlFunctionExpr0<R>(name, type)
