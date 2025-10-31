package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.Codec.Companion.createType
import kotlin.reflect.KClass

fun <T> Codec.getAwaker(type: TypeRef<T>): Awaker {
    return getAwaker(type.type)
}

inline fun <reified T> Codec.getAwaker(): Awaker {
    return getAwaker(kType<T>().type)
}

fun <T : Any> Codec.awake(cls: KClass<T>, data: Any?): T? {
//        contract {
//            (data != null) implies (returnsNotNull())
//        }

    @Suppress("UNCHECKED_CAST")
    return awake(cls.createType(), data) as T?
}

fun <T> Codec.awake(type: TypeRef<T>, data: Any?): T? {
//        contract {
//            (data != null) implies (returnsNotNull())
//        }

    @Suppress("UNCHECKED_CAST")
    return awake(type.type, data) as T?
}

inline fun <reified T> Codec.awake(data: Any?): T? {
//        contract {
//            (data != null) implies (returnsNotNull())
//        }
    return awake(kType<T>().type, data) as T?
}


fun <T> Codec.getSlumberer(type: TypeRef<T>): Slumberer {
    return getSlumberer(type.type)
}

inline fun <reified T> Codec.getSlumberer(): Slumberer {
    return getSlumberer(kType<T>().type)
}

fun Codec.slumber(data: Any?): Any? {
//        contract {
//            (data != null) implies (returnsNotNull())
//        }
    val cls = when {
        data != null -> data::class
        else -> Nothing::class
    }

    return slumber(cls, data)
}

fun <T : Any> Codec.slumber(targetType: KClass<T>, data: Any?): T? {
//            contract {
//                (data != null) implies (returnsNotNull())
//            }

    @Suppress("UNCHECKED_CAST")
    return slumber(targetType.createType(), data) as T?
}
