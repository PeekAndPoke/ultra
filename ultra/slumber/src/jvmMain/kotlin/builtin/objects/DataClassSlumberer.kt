package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.cache.Cache
import io.peekandpoke.ultra.cache.FastCache
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.reflection.hasAnyAnnotationOnPropertyDefinedOnSuperTypes
import io.peekandpoke.ultra.reflection.hasAnyAnnotationRecursive
import io.peekandpoke.ultra.slumber.Slumber
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

/**
 * Serializes data class instances into Maps by iterating over constructor properties plus any
 * property carrying [Slumber.Field].
 *
 * `null` input slumbers to `null`; a surrounding `NonNullSlumberer` turns that into a
 * `SlumbererException` for a non-nullable declared type.
 *
 * A [SlumberCache] put into the [SlumberConfig] attributes (see `withSlumberCache`) makes the
 * results memoizable — see the TODOs on `Cached` for what that currently costs.
 */
interface DataClassSlumberer : Slumberer {

    companion object {
        private val slumberCacheKey = TypedKey<SlumberCache>("DataClassSlumber.ValueCache")

        // TODO(scan): the cache is read once, when the slumberer is built, and SlumberConfig.Lookup
        //   memoizes that slumberer per KType across copy() - so whichever config resolved a type
        //   FIRST decides, for every config sharing the Lookup, whether it caches at all.
        /** Creates a [DataClassSlumberer] for the given [type], optionally backed by a [SlumberCache]. */
        operator fun invoke(type: KType, attributes: TypedAttributes): DataClassSlumberer {
            val default = Default(type)

            return when (val cache = attributes.getSlumberCache()) {
                null -> default
                else -> Cached(default, cache)
            }
        }

        /** Installs a [SlumberCache] built by [builder], excluding no classes. */
        fun SlumberConfig.withSlumberCache(
            builder: FastCache.Builder<Any?, Any?>.() -> Unit,
        ): SlumberConfig = withSlumberCache(excludedClasses = emptySet(), builder)

        /**
         * Installs a [SlumberCache] built by [builder] that never caches instances of [excludedClasses].
         *
         * An empty [builder] yields a cache with no eviction behaviour at all, i.e. one that grows
         * without bound — pass at least `maxEntries` or `maxMemoryUsage`.
         */
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

        // TODO(scan): appendModules/prependModules do not carry attributes over, so calling either
        //   AFTER this silently drops the cache again.
        /** Installs the given [cache] into the config's attributes. */
        fun SlumberConfig.withSlumberCache(cache: SlumberCache): SlumberConfig {
            return copy(
                attributes = attributes.plus(slumberCacheKey, cache)
            )
        }

        /** Returns the [SlumberCache] installed by `withSlumberCache`, or `null` when there is none. */
        fun TypedAttributes.getSlumberCache(): Cache<Any?, Any?>? = this[slumberCacheKey]
    }

    /**
     * Cache wrapper that delegates to [wrapped] and refuses to cache instances of [excludedClasses].
     *
     * Exclusion matches the key's EXACT runtime class, not subtypes: excluding a sealed parent does
     * not exclude its children.
     */
    class SlumberCache(
        val wrapped: Cache<Any?, Any?>,
        val excludedClasses: Set<KClass<*>> = emptySet(),
    ) : Cache<Any?, Any?> {
        override val keys: Set<Any?> get() = wrapped.keys
        override val values: List<Any?> get() = wrapped.values
        override val entries: Map<Any?, Any?> get() = wrapped.entries
        override val size: Int get() = wrapped.size

        override fun clear() {
            wrapped.clear()
        }

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
            if (key == null || key::class in excludedClasses) {
                return producer()
            }

            return wrapped.getOrPut(key, producer)
        }

        // TODO(scan): the only override that skips the null / excludedClasses guard the other four honour.
        override fun remove(key: Any?): Any? {
            return wrapped.remove(key)
        }
    }

    private class Cached(
        private val wrapped: DataClassSlumberer,
        private val cache: Cache<Any?, Any?>,
    ) : DataClassSlumberer {

        // TODO(scan): keyed on `data` alone. Three consequences: (1) `context` is ignored, so one cache
        //   instance shared by two configs serves each other's shapes; (2) equality is the key, so two
        //   data-class instances that are equals() but differ in a @Slumber.Field non-ctor property get
        //   the same map; (3) keys are held strongly and hashed deeply on every call.
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

        /** Ctor-backed properties plus every property annotated with [Slumber.Field], de-duplicated. */
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

        // TODO(scan): no context.stepInto(prop.name) here, unlike the collection and map slumberers, so
        //   a SlumbererException from a nested field always reports the path as 'root'.
        // TODO(scan): returns a live MutableMap; once Cached hands the same instance out repeatedly, a
        //   caller that mutates the result corrupts the cache entry.
        override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? {
            if (data == null) {
                return null
            }

            val result = mutableMapOf<String, Any?>()

            allSlumberFields.forEach { (prop, _) ->
                // The declared type is dropped here: context.slumber dispatches on the RUNTIME class.
                result[prop.name] = context.slumber(prop.get(data))
            }

            return result
        }
    }
}
