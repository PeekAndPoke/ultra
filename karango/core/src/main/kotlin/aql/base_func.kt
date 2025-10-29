package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType

inline fun <reified R> aqlFunc(name: String) = AqlFunctionDefinition<R>(name = name, type = kType())

enum class AqlFunc {
    // TODO: all date functions

    // TODO: special Database functions
    COLLECTION_COUNT,
    COLLECTIONS,
    HASH,
    APPLY,
    ASSERT,
    WARN,
    CALL,
    FAIL,
    NOOPT,
    PASSTHRU,
    SLEEP,
    V8,
    VERSION,

    // TODO: Document / Object functions

    // TODO: Fulltext functions

    // TODO: Geo functions

    // TODO: Miscellaneous Functions
    FIRST_LIST,
    FIRST_DOCUMENT,
    NOT_NULL,

    // Numeric

    // TODO: Strings Functions
    REGEX_REPLACE, // TODO: impl, tests
    SUBSTITUTE, // TODO: impl, tests
    TOKENS, // TODO: impl, tests
}

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
        AqlFunctionCall.of(type = type, func = getName(), args = args) as AqlExpression<X>
}

interface AqlFunctionCall<T> : AqlExpression<T> {
    companion object {
        fun <X> of(type: TypeRef<X>, func: String, vararg args: AqlExpression<*>): AqlExpression<X> =
            Impl(type, func, args)
    }

    private class Impl<T>(
        private val type: TypeRef<T>,
        private val func: String,
        private val args: Array<out AqlExpression<*>>,
    ) : AqlFunctionCall<T> {

        override fun getType() = type

        override fun print(p: AqlPrinter) {
            p.append("${func}(").join(args).append(")")
        }
    }
}

