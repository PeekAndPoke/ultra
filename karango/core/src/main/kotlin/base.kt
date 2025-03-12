@file:Suppress("ObjectPropertyName", "unused")

package de.peekandpoke.karango

import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.PathExpr
import de.peekandpoke.ultra.vault.lang.property

/** Refers to the _id of a stored document */
inline val <T> Expression<T>._id: PathExpr<String>
    inline get() = property("_id")

/** Refers to the _key of a stored document */
inline val <T> Expression<T>._key: PathExpr<String>
    inline get() = property("_key")

/** Refers to the _rev of a stored document */
inline val <T> Expression<T>._rev: PathExpr<String>
    inline get() = property("_rev")
