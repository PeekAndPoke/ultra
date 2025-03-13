package de.peekandpoke.ultra.vault.slumber

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.IterableExpr
import de.peekandpoke.ultra.vault.lang.PathExpr
import de.peekandpoke.ultra.vault.lang.PropertyPath
import de.peekandpoke.ultra.vault.lang.property

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> IterableExpr<T>._type
    inline get() = PropertyPath.start(this).property<String>("_type")

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> PathExpr<T>._type
    inline get() = property<String>("_type")

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> Expression<T>._type
    inline get() = property<String>("_type")

/** Helper to get the "ts" property of serialized datetime objects */
inline val PathExpr<MpInstant>.ts
    @JvmName("ts_MpInstant") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val PathExpr<MpZonedDateTime>.ts
    @JvmName("ts_MpZonedDateTime") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val PathExpr<MpLocalDateTime>.ts
    @JvmName("ts_MpLocalDateTime") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val PathExpr<MpLocalDate>.ts
    @JvmName("ts_MpLocalDate") inline get() = property<Long>("ts")
