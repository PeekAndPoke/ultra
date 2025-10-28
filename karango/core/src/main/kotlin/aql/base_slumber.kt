package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> AqlIterableExpr<T>._type
    inline get() = AqlPropertyPath.start(this).property<String>("_type")

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> AqlPathExpr<T>._type
    inline get() = property<String>("_type")

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> AqlExpression<T>._type
    inline get() = property<String>("_type")

/** Helper to get the "ts" property of serialized datetime objects */
inline val AqlPathExpr<MpInstant>.ts
    @JvmName("ts_MpInstant") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val AqlPathExpr<MpZonedDateTime>.ts
    @JvmName("ts_MpZonedDateTime") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val AqlPathExpr<MpLocalDateTime>.ts
    @JvmName("ts_MpLocalDateTime") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val AqlPathExpr<MpLocalDate>.ts
    @JvmName("ts_MpLocalDate") inline get() = property<Long>("ts")
