package io.peekandpoke.karango.aql

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
