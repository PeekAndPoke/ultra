package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef

abstract class FunctionExpr<R>(private val name: String, private val type: TypeRef<R>) : Expression<R> {
    override fun getType(): TypeRef<R> = type

    override fun print(p: Printer): Any = with(p) {
        append(name)
    }
}

abstract class FunctionExpr0<R>(name: String, type: TypeRef<R>) : FunctionExpr<R>(name, type)

abstract class FunctionExpr1<P1, R>(name: String, type: TypeRef<R>) : FunctionExpr0<R>(name, type)
