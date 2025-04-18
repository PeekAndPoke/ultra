package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.FunctionExpr0
import de.peekandpoke.ultra.vault.lang.Printer

object Aql {
    object COUNT : FunctionExpr0<Int>(AqlFunc.COUNT.toString(), kType())
}

enum class AqlFunc {
    // Overloaded functions
    LENGTH, // TODO: impl, tests FOR Collections
    COUNT, // TODO: impl, tests FOR Collections
    REVERSE, // TODO: impl, tests FOR Collections

    // Array
    APPEND,
    CONTAINS_ARRAY,
    COUNT_DISTINCT,
    COUNT_UNIQUE,
    FIRST,
    FLATTEN,
    INTERSECTION,
    LAST,
    MINUS,
    NTH,
    OUTERSECTION,
    POP,
    POSITION,
    PUSH,
    REMOVE_NTH,
    REMOVE_VALUE,
    REMOVE_VALUES,
    SHIFT,
    SLICE,
    SORTED,
    SORTED_UNIQUE,
    UNION,
    UNION_DISTINCT,
    UNIQUE,
    UNSHIFT,

    // Date TODO

    // Database functions
    COLLECTION_COUNT, // TODO: impl, tests
    COLLECTIONS, // TODO: impl, tests
    DOCUMENT, // TODO: tests
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

    // Document / Object TODO

    MERGE, // TODO: tests
    UNSET, // TODO: Tests

    // Fulltext TODO

    // Geo // TODO

    // Miscellaneous
    FIRST_LIST, // TODO: impl, tests
    FIRST_DOCUMENT, // TODO: impl, tests
    NOT, // TODO: tests
    NOT_NULL, // TODO: impl, tests

    // Numeric
    ABS,
    ACOS,
    ASIN,
    ATAN,
    ATAN2,
    AVERAGE,
    AVG,
    CEIL,
    COS,
    DEGREES,
    EXP,
    EXP2,
    FLOOR,
    LOG,
    LOG2,
    LOG10,
    MAX,
    MEDIAN,
    MIN,
    PERCENTILE,
    PI,
    POW,
    RADIANS,
    RAND,
    RANGE,
    ROUND,
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
    CHAR_LENGTH,
    CONCAT,
    CONCAT_SEPARATOR,
    CONTAINS,
    ENCODE_URI_COMPONENT,
    FIND_FIRST,
    FIND_LAST,
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
    REGEX_SPLIT, // TODO: impl, tests
    REGEX_TEST, // TODO: impl, tests
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

    // Type checks
    IS_NULL,
    IS_BOOL,
    IS_NUMBER,
    IS_STRING,
    IS_ARRAY,
    IS_LIST,
    IS_OBJECT,
    IS_DOCUMENT,
    IS_DATESTRING,
    IS_KEY,
    TYPENAME,

    // Type Conversion
    TO_BOOL,
    TO_NUMBER,
    TO_STRING,
    TO_ARRAY,
    TO_LIST,
}

fun <T> AqlFunc.call(type: TypeRef<T>, vararg args: Expression<*>) = FuncCall.of(type, this, args)
fun <T> AqlFunc.nullableCall(type: TypeRef<T?>, vararg args: Expression<*>) = FuncCall.of(type, this, args)

fun AqlFunc.anyCall(vararg args: Expression<*>) = FuncCall.any(this, args)
fun AqlFunc.nullableAnyCall(vararg args: Expression<*>) = FuncCall.nullableAny(this, args)

fun <T> AqlFunc.arrayCall(type: TypeRef<List<T>>, vararg args: Expression<*>) = FuncCall.array(this, type, args)
fun <T> AqlFunc.nullableArrayCall(type: TypeRef<List<T>?>, vararg args: Expression<*>) =
    FuncCall.nullableArray(this, type, args)

fun AqlFunc.boolCall(vararg args: Expression<*>) = FuncCall.bool(this, args)

fun AqlFunc.numberCall(vararg args: Expression<*>) = FuncCall.number(this, args)
fun AqlFunc.nullableNumberCall(vararg args: Expression<*>) = FuncCall.nullableNumber(this, args)

fun AqlFunc.stringCall(vararg args: Expression<*>) = FuncCall.string(this, args)

interface FuncCall<T> : Expression<T> {

    companion object {

        fun <X> of(type: TypeRef<X>, func: AqlFunc, args: Array<out Expression<*>>): Expression<X> =
            FuncCallImpl(type, func, args)

        fun any(func: AqlFunc, args: Array<out Expression<*>>): Expression<Any> = FuncCallImpl(TypeRef.Any, func, args)
        fun nullableAny(func: AqlFunc, args: Array<out Expression<*>>): Expression<Any?> =
            FuncCallImpl(TypeRef.AnyNull, func, args)

        fun <T> array(func: AqlFunc, type: TypeRef<List<T>>, args: Array<out Expression<*>>): FuncCall<List<T>> =
            FuncCallImpl(type, func, args)

        fun <T> nullableArray(
            func: AqlFunc,
            type: TypeRef<List<T>?>,
            args: Array<out Expression<*>>,
        ): FuncCall<List<T>?> = FuncCallImpl(type, func, args)

        fun bool(func: AqlFunc, args: Array<out Expression<*>>): Expression<Boolean> =
            FuncCallImpl(TypeRef.Boolean, func, args)

        fun number(func: AqlFunc, args: Array<out Expression<*>>): Expression<Number> =
            FuncCallImpl(TypeRef.Number, func, args)

        fun nullableNumber(func: AqlFunc, args: Array<out Expression<*>>): Expression<Number?> =
            FuncCallImpl(TypeRef.NumberNull, func, args)

        fun string(func: AqlFunc, args: Array<out Expression<*>>): Expression<String> =
            FuncCallImpl(TypeRef.String, func, args)
    }
}

internal class FuncCallImpl<T>(
    private val type: TypeRef<T>,
    private val func: AqlFunc,
    private val args: Array<out Expression<*>>,
) : FuncCall<T> {

    override fun getType() = type
    override fun print(p: Printer) = p.append("${func.name}(").join(args).append(")")
}
