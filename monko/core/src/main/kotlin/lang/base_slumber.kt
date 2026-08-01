package io.peekandpoke.monko.lang

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
