package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.ServiceProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * Counts constructor runs per class, independently of what the kontainer tracks.
 *
 * Tests call [reset] first. Kotest runs the tests of this module sequentially.
 */
object Births {
    private val counts = ConcurrentHashMap<KClass<*>, AtomicInteger>()

    fun reset() = counts.clear()

    fun record(instance: Any) {
        counts.getOrPut(instance::class) { AtomicInteger() }.incrementAndGet()
    }

    fun of(cls: KClass<*>): Int = counts[cls]?.get() ?: 0
}

/** Base for services whose constructor runs are counted by [Births]. */
abstract class Counted {
    init {
        @Suppress("LeakingThis")
        Births.record(this)
    }
}

/** The three ways a service can be declared today. */
enum class Declared { Singleton, Prototype, Dynamic }

/** The service builder for [declared]. */
fun KontainerBuilder.declare(declared: Declared): KontainerBuilder.ServiceBuilder = when (declared) {
    Declared.Singleton -> singleton
    Declared.Prototype -> prototype
    Declared.Dynamic -> dynamic
}

/** The provider type the kontainer reports for [cls]. */
fun <T : Any> Kontainer.typeOf(cls: KClass<T>): ServiceProvider.Type = getProvider(cls).type
