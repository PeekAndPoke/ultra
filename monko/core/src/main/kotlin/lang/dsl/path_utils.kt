package io.peekandpoke.monko.lang.dsl

import io.peekandpoke.monko.lang.MongoPrinter.Companion.printQuery
import io.peekandpoke.monko.lang.MongoPropertyPath

/**
 * Extracts the dot-notation field path string from a [MongoPropertyPath].
 *
 * This is the bridge between KSP-generated type-safe property paths and MongoDB's
 * string-based field references. For example, a path representing `r.address.city`
 * will return `"address.city"`.
 */
fun MongoPropertyPath<*, *>.toFieldPath(): String {
    val dropped = dropRoot() ?: return ""

    return dropped.getAsList().joinToString(".") {
        it.printQuery().replace("`", "")
    }
}
