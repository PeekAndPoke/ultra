package de.peekandpoke.monko.lang

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> MongoIterableExpr<T>._type
    inline get() = MongoPropertyPath.start(this).property<String>("_type")

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> MongoPathExpr<T>._type
    inline get() = property<String>("_type")

/** Refers to the default polymorphic type discriminator "_type" */
@Suppress("ObjectPropertyName")
inline val <T> MongoExpression<T>._type
    inline get() = property<String>("_type")

/** Helper to get the "ts" property of serialized datetime objects */
inline val MongoPathExpr<MpInstant>.ts
    @JvmName("ts_MpInstant") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val MongoPathExpr<MpZonedDateTime>.ts
    @JvmName("ts_MpZonedDateTime") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val MongoPathExpr<MpLocalDateTime>.ts
    @JvmName("ts_MpLocalDateTime") inline get() = property<Long>("ts")

/** Helper to get the "ts" property of serialized datetime objects */
inline val MongoPathExpr<MpLocalDate>.ts
    @JvmName("ts_MpLocalDate") inline get() = property<Long>("ts")
