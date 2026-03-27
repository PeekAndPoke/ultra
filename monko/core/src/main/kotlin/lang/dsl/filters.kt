package io.peekandpoke.monko.lang.dsl

import com.mongodb.client.model.Filters
import io.peekandpoke.monko.lang.MongoPropertyPath
import org.bson.conversions.Bson

// Comparison operators ////////////////////////////////////////////////////////////////////////////////////////////

/** Matches documents where the field equals the given [value]. */
infix fun <T> MongoPropertyPath<*, T>.eq(value: T): Bson =
    Filters.eq(toFieldPath(), value)

/** Matches documents where the field does not equal the given [value]. */
infix fun <T> MongoPropertyPath<*, T>.ne(value: T): Bson =
    Filters.ne(toFieldPath(), value)

/** Matches documents where the field is greater than the given [value]. */
infix fun <T : Comparable<T>> MongoPropertyPath<*, T>.gt(value: T): Bson =
    Filters.gt(toFieldPath(), value)

/** Matches documents where the field is greater than or equal to the given [value]. */
infix fun <T : Comparable<T>> MongoPropertyPath<*, T>.gte(value: T): Bson =
    Filters.gte(toFieldPath(), value)

/** Matches documents where the field is less than the given [value]. */
infix fun <T : Comparable<T>> MongoPropertyPath<*, T>.lt(value: T): Bson =
    Filters.lt(toFieldPath(), value)

/** Matches documents where the field is less than or equal to the given [value]. */
infix fun <T : Comparable<T>> MongoPropertyPath<*, T>.lte(value: T): Bson =
    Filters.lte(toFieldPath(), value)

// Collection membership operators /////////////////////////////////////////////////////////////////////////////////

/** Matches documents where the field's value is in the given [values]. */
infix fun <T> MongoPropertyPath<*, T>.isIn(values: Collection<T>): Bson =
    Filters.`in`(toFieldPath(), values)

/** Matches documents where the field's value is not in the given [values]. */
infix fun <T> MongoPropertyPath<*, T>.nin(values: Collection<T>): Bson =
    Filters.nin(toFieldPath(), values)

// String operators ////////////////////////////////////////////////////////////////////////////////////////////////

/** Matches documents where the string field matches the given regex [pattern]. */
infix fun MongoPropertyPath<*, String>.regex(pattern: String): Bson =
    Filters.regex(toFieldPath(), pattern)

/** Matches documents where the string field matches the given [regex]. */
infix fun MongoPropertyPath<*, String>.regex(regex: Regex): Bson =
    Filters.regex(toFieldPath(), regex.toPattern())

// Existence operators /////////////////////////////////////////////////////////////////////////////////////////////

/** Matches documents where the field exists (or does not exist if [value] is false). */
fun MongoPropertyPath<*, *>.exists(value: Boolean = true): Bson =
    Filters.exists(toFieldPath(), value)

// Array operators /////////////////////////////////////////////////////////////////////////////////////////////////

/** Matches documents where at least one element in the array field matches the given [filter]. */
fun MongoPropertyPath<*, *>.elemMatch(filter: Bson): Bson =
    Filters.elemMatch(toFieldPath(), filter)

/** Matches documents where the array field has the given [size]. */
fun MongoPropertyPath<*, *>.size(size: Int): Bson =
    Filters.size(toFieldPath(), size)

/** Matches documents where the array field contains all of the given [values]. */
infix fun <T> MongoPropertyPath<*, out Collection<T>>.all(values: Collection<T>): Bson =
    Filters.all(toFieldPath(), values)

// Logical combinators /////////////////////////////////////////////////////////////////////////////////////////////

/** Combines multiple filters with a logical AND. */
fun and(vararg filters: Bson): Bson = Filters.and(*filters)

/** Combines multiple filters with a logical AND. */
fun and(filters: List<Bson>): Bson = Filters.and(filters)

/** Combines multiple filters with a logical OR. */
fun or(vararg filters: Bson): Bson = Filters.or(*filters)

/** Combines multiple filters with a logical OR. */
fun or(filters: List<Bson>): Bson = Filters.or(filters)

/** Negates the given [filter]. */
fun not(filter: Bson): Bson = Filters.not(filter)

/** Combines multiple filters with a logical NOR. */
fun nor(vararg filters: Bson): Bson = Filters.nor(*filters)
