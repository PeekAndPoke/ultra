package io.peekandpoke.monko.lang.dsl

import com.mongodb.client.model.Sorts
import io.peekandpoke.monko.lang.MongoPropertyPath
import org.bson.conversions.Bson

/** Sort ascending by this field. */
val MongoPropertyPath<*, *>.asc: Bson
    get() = Sorts.ascending(toFieldPath())

/** Sort descending by this field. */
val MongoPropertyPath<*, *>.desc: Bson
    get() = Sorts.descending(toFieldPath())

/** Combine multiple sort specifications into one. */
fun orderBy(vararg sorts: Bson): Bson = Sorts.orderBy(*sorts)
