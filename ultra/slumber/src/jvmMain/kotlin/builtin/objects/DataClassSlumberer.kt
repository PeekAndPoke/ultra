package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.cache.Cache
import de.peekandpoke.ultra.common.cache.FastCache
import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.common.reflection.hasAnyAnnotationOnPropertyDefinedOnSuperTypes
import de.peekandpoke.ultra.common.reflection.hasAnyAnnotationRecursive
import de.peekandpoke.ultra.slumber.Slumber
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

interface DataClassSlumberer : Slumberer {

    companion object {
        private val slumberCacheKey = TypedKey<SlumberCache>("DataClassSlumber.ValueCache")

        operator fun invoke(type: KType, attributes: TypedAttributes): DataClassSlumberer {
            val default = Default(type)

            return when (val cache = attributes.getSlumberCache()) {
                null -> default
                else -> Cached(default, cache)
            }
        }

        fun SlumberConfig.withSlumberCache(
            builder: FastCache.Builder<Any?, Any?>.() -> Unit,
        ): SlumberConfig = withSlumberCache(excludedClasses = emptySet(), builder)

        fun SlumberConfig.withSlumberCache(
            excludedClasses: Set<KClass<*>>,
            builder: FastCache.Builder<Any?, Any?>.() -> Unit,
        ): SlumberConfig {
            val cache = FastCache.Builder<Any?, Any?>().apply(builder).build()

            return withSlumberCache(
                SlumberCache(
                    wrapped = cache,
                    excludedClasses = excludedClasses,
                )
            )
        }

        fun SlumberConfig.withSlumberCache(cache: SlumberCache): SlumberConfig {
            return copy(
                attributes = attributes.plus(slumberCacheKey, cache)
            )
        }

        fun TypedAttributes.getSlumberCache(): Cache<Any?, Any?>? = this[slumberCacheKey]
    }

    class SlumberCache(
        val wrapped: Cache<Any?, Any?>,
        val excludedClasses: Set<KClass<*>> = emptySet(),
    ) : Cache<Any?, Any?> {
        override fun get(key: Any?): Any? {
            if (key == null || key::class in excludedClasses) {
                return null
            }

            return wrapped.get(key)
        }

        override fun has(key: Any?): Boolean {
            if (key == null || key::class in excludedClasses) {
                return false
            }

            return wrapped.has(key)
        }

        override fun put(key: Any?, value: Any?) {
            if (key == null || key::class in excludedClasses) {
                return
            }

            wrapped.put(key, value)
        }

        override fun getOrPut(key: Any?, producer: () -> Any?): Any? {
            return wrapped.getOrPut(key, producer)
        }

        override fun remove(key: Any?): Any? {
            return wrapped.remove(key)
        }
    }

    private class Cached(
        private val wrapped: DataClassSlumberer,
        private val cache: Cache<Any?, Any?>,
    ) : DataClassSlumberer {

        override fun slumber(data: Any?, context: Slumberer.Context): Any? {
            return cache.getOrPut(data) {
                wrapped.slumber(data, context)
            }
        }
    }

    private class Default(type: KType) : DataClassSlumberer {
        /** Raw cls of the rootType */
        val reified = ReifiedKType(type)

        /** Gets the primary Ctor */
        val primaryCtor = reified.ctor

        /** All fields that need to be slumbered */
        val allSlumberFields = reified.ctorFields2Types
            .plus(
                reified.allPropertiesToTypes.filter { (prop, _) ->
                    // Include all fields for serialization that have the @Slumber.Field directly
                    prop.hasAnyAnnotationRecursive { it is Slumber.Field } ||
                            // Also include fields that have the @Slumber.Field defined in a parent
                            prop.hasAnyAnnotationOnPropertyDefinedOnSuperTypes(reified.cls) { it is Slumber.Field }
                }
            )
            .distinctBy { (prop, _) -> prop }

        init {
            // We need to make all constructors accessible.
            // This is necessary so that overloaded constructors with default values can be called correctly.
            // We need to also make the java underlying java methods accessible, as the Kotlin impl is sometimes buggy.
            primaryCtor?.isAccessible = true
            primaryCtor?.javaMethod?.isAccessible = true
            primaryCtor?.javaConstructor?.isAccessible = true

            reified.cls.constructors.forEach {
                it.isAccessible = true
                it.javaMethod?.isAccessible = true
                it.javaConstructor?.isAccessible = true
            }
        }

        override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? {
            if (data == null) {
                return null
            }

            val result = mutableMapOf<String, Any?>()

            allSlumberFields.forEach { (prop, _) ->
                result[prop.name] = context.slumber(prop.get(data))
            }

            return result
        }
    }
}
