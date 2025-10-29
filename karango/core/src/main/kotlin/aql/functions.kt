@file:Suppress("TooManyFunctions")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.lang.VaultInputValueMarker

enum class AqlPercentileMethod(val method: String) {
    @VaultInputValueMarker
    RANK("rank"),

    @VaultInputValueMarker
    INTERPOLATION("interpolation")
}

enum class AqlFunc {
    // TODO: all date functions

    // TODO: special Database functions
    COLLECTION_COUNT, // TODO: impl, tests
    COLLECTIONS, // TODO: impl, tests
    HASH, // TODO: impl, tests
    APPLY, // TODO: impl, tests
    ASSERT, // TODO: impl, tests
    WARN, // TODO: impl, tests
    CALL, // TODO: impl, tests
    FAIL, // TODO: impl, tests
    NOOPT, // TODO: impl, tests
    PASSTHRU, // TODO: impl, tests
    SLEEP, // TODO: impl, tests
    V8, // TODO: impl, tests
    VERSION, // TODO: impl, tests

    // TODO: Document / Object functions

    // TODO: Fulltext functions

    // TODO: Geo functions

    // Miscellaneous
    FIRST_LIST, // TODO: impl, tests
    FIRST_DOCUMENT, // TODO: impl, tests
    NOT, // TODO: tests
    NOT_NULL, // TODO: impl, tests

    // Numeric
    SIN,
    SQRT,
    STDDEV_POPULATION,
    STDDEV_SAMPLE,
    STDDEV,
    SUM,
    TAN,
    VARIANCE_POPULATION,
    VARIANCE_SAMPLE,
    VARIANCE,

    // Strings
    JSON_PARSE,
    JSON_STRINGIFY,
    LEFT,
    LEVENSHTEIN_DISTANCE,
    LIKE,
    LOWER,
    LTRIM,
    MD5,
    RANDOM_TOKEN,
    REGEX_MATCHES,
    REGEX_SPLIT,
    REGEX_TEST,
    REGEX_REPLACE, // TODO: impl, tests
    RIGHT,
    RTRIM,
    SHA1,
    SHA512,
    SPLIT,
    SOUNDEX,
    SUBSTITUTE, // TODO: impl, tests
    SUBSTRING,
    TOKENS, // TODO: impl, tests
    TO_BASE64,
    TO_HEX,
    TRIM,
    UPPER,
    UUID,
    STARTS_WITH, // TODO: tests
}

fun <T> AqlFunc.call(type: TypeRef<T>, vararg args: AqlExpression<*>): AqlExpression<T> =
    AqlFuncCall.of(type, this, args)

fun <T> AqlFunc.nullableCall(type: TypeRef<T?>, vararg args: AqlExpression<*>): AqlExpression<T?> =
    AqlFuncCall.of(type, this, args)

fun AqlFunc.anyCall(vararg args: AqlExpression<*>): AqlExpression<Any> =
    AqlFuncCall.any(this, args)

fun AqlFunc.nullableAnyCall(vararg args: AqlExpression<*>): AqlExpression<Any?> =
    AqlFuncCall.nullableAny(this, args)

fun <T> AqlFunc.arrayCall(type: TypeRef<List<T>>, vararg args: AqlExpression<*>): AqlExpression<List<T>> =
    AqlFuncCall.array(this, type, args)

fun <T> AqlFunc.nullableArrayCall(type: TypeRef<List<T>?>, vararg args: AqlExpression<*>): AqlExpression<List<T>?> =
    AqlFuncCall.nullableArray(this, type, args)

fun AqlFunc.boolCall(vararg args: AqlExpression<*>): AqlExpression<Boolean> =
    AqlFuncCall.bool(this, args)

fun AqlFunc.nullableBoolCall(vararg args: AqlExpression<*>): AqlExpression<Boolean?> =
    AqlFuncCall.nullableBool(this, args)

fun AqlFunc.numberCall(vararg args: AqlExpression<*>): AqlExpression<Number> =
    AqlFuncCall.number(this, args)

fun AqlFunc.nullableNumberCall(vararg args: AqlExpression<*>): AqlExpression<Number?> =
    AqlFuncCall.nullableNumber(this, args)

fun AqlFunc.stringCall(vararg args: AqlExpression<*>): AqlExpression<String> =
    AqlFuncCall.string(this, args)

fun AqlFunc.nullableStringCall(vararg args: AqlExpression<*>): AqlExpression<String?> =
    AqlFuncCall.nullableString(this, args)

interface AqlFuncCall<T> : AqlExpression<T> {

    companion object {

        fun <X> of(type: TypeRef<X>, func: String, vararg args: AqlExpression<*>): AqlExpression<X> =
            AqlFuncCallImpl(type, func, args)

        fun <X> of(type: TypeRef<X>, func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<X> =
            AqlFuncCallImpl(type, func, args)

        fun any(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<Any> =
            AqlFuncCallImpl(TypeRef.Any, func, args)

        fun nullableAny(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<Any?> =
            AqlFuncCallImpl(TypeRef.AnyNull, func, args)

        fun <T> array(func: AqlFunc, type: TypeRef<List<T>>, args: Array<out AqlExpression<*>>)
                : AqlExpression<List<T>> = AqlFuncCallImpl(type, func, args)

        fun <T> nullableArray(func: AqlFunc, type: TypeRef<List<T>?>, args: Array<out AqlExpression<*>>)
                : AqlExpression<List<T>?> = AqlFuncCallImpl(type, func, args)

        fun bool(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<Boolean> =
            AqlFuncCallImpl(TypeRef.Boolean, func, args)

        fun nullableBool(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<Boolean?> =
            AqlFuncCallImpl(TypeRef.BooleanNull, func, args)

        fun number(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<Number> =
            AqlFuncCallImpl(TypeRef.Number, func, args)

        fun nullableNumber(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<Number?> =
            AqlFuncCallImpl(TypeRef.NumberNull, func, args)

        fun string(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<String> =
            AqlFuncCallImpl(TypeRef.String, func, args)

        fun nullableString(func: AqlFunc, args: Array<out AqlExpression<*>>): AqlExpression<String?> =
            AqlFuncCallImpl(TypeRef.StringNull, func, args)
    }
}

internal class AqlFuncCallImpl<T>(
    private val type: TypeRef<T>,
    private val func: String,
    private val args: Array<out AqlExpression<*>>,
) : AqlFuncCall<T> {

    constructor(type: TypeRef<T>, func: AqlFunc, args: Array<out AqlExpression<*>>) : this(type, func.name, args)

    override fun getType() = type

    override fun print(p: AqlPrinter) {
        p.append("${func}(").join(args).append(")")
    }
}
