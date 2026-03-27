package io.peekandpoke.monko.lang.dsl

import com.mongodb.client.model.Updates
import io.peekandpoke.monko.lang.MongoPropertyPath
import org.bson.conversions.Bson

/** Set the field to the given [value]. */
infix fun <T> MongoPropertyPath<*, T>.setTo(value: T): Bson =
    Updates.set(toFieldPath(), value)

/** Remove the field from the document. */
fun MongoPropertyPath<*, *>.unset(): Bson =
    Updates.unset(toFieldPath())

/** Increment the field by the given integer [value]. */
infix fun MongoPropertyPath<*, Int>.inc(value: Int): Bson =
    Updates.inc(toFieldPath(), value)

/** Increment the field by the given long [value]. */
infix fun MongoPropertyPath<*, Long>.inc(value: Long): Bson =
    Updates.inc(toFieldPath(), value)

/** Increment the field by the given double [value]. */
infix fun MongoPropertyPath<*, Double>.inc(value: Double): Bson =
    Updates.inc(toFieldPath(), value)

/** Push a [value] onto the array field. */
infix fun <T> MongoPropertyPath<*, out Collection<T>>.push(value: T): Bson =
    Updates.push(toFieldPath(), value)

/** Remove the first occurrence of [value] from the array field. */
infix fun <T> MongoPropertyPath<*, out Collection<T>>.pull(value: T): Bson =
    Updates.pull(toFieldPath(), value)

/** Add [value] to the array field only if it doesn't already exist. */
infix fun <T> MongoPropertyPath<*, out Collection<T>>.addToSet(value: T): Bson =
    Updates.addToSet(toFieldPath(), value)

/** Combine multiple update operations into one. */
fun combine(vararg updates: Bson): Bson = Updates.combine(*updates)
