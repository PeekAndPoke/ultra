package io.peekandpoke.funktor.insights

import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.reflect.KClass

/** A small real kontainer with one service of every provider type the insights graphs distinguish. */
object KontainerFixture {

    class Clock
    class Unused
    class Stamp

    interface Greeter
    class DefaultGreeter : Greeter
    class OtherGreeter : Greeter
    class RequestGreeter : Greeter

    /** A singleton that injects a dynamic, so the kontainer downgrades it to `SemiDynamic`. */
    class Wrapper(val greeter: Greeter)

    class Handler(val clock: Clock, val greeter: Greeter, val later: Lazy<Clock>)

    class Missing
    class Needy(val missing: Missing)

    /** One of each: Singleton, never-instantiated Singleton, Prototype, Dynamic, SemiDynamic. */
    fun blueprint(): KontainerBlueprint = kontainer { services() }

    /**
     * As [blueprint], with [Greeter] registered a second time, so its definition carries `overwrites`.
     *
     * In the SAME builder: `KontainerBlueprint.extend` replaces a definition without recording what it replaced.
     */
    fun blueprintWithOverwrite(): KontainerBlueprint = kontainer {
        services()
        dynamic(Greeter::class) { OtherGreeter() }
    }

    private fun KontainerBuilder.services() {
        singleton(Clock::class)
        singleton(Unused::class)
        prototype(Stamp::class)
        dynamic(Greeter::class) { DefaultGreeter() }
        singleton(Wrapper::class)
        dynamic(Handler::class)
    }

    /**
     * Injects a service nobody registered, so its first `create()` fails validation: a graph that cannot be built.
     *
     * Only the FIRST: the blueprint marks itself validated before validating, so a second `create()` succeeds.
     */
    fun brokenBlueprint(): KontainerBlueprint = kontainer { singleton(Needy::class) }

    /** Registers [classes] as singletons from ONE line, so every definition has the same code location. */
    fun singletons(vararg classes: KClass<out Any>): KontainerBlueprint = kontainer {
        classes.forEach { register(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun KontainerBuilder.register(cls: KClass<out Any>) = singleton(cls as KClass<Any>)

    /** What a request's kontainer looks like: [Greeter] handed in at creation, [Handler] instantiated. */
    fun requestKontainer(blueprint: KontainerBlueprint = blueprint()): Kontainer =
        blueprint.create { with(Greeter::class) { RequestGreeter() } }
            .also { it.get(Handler::class) }
}
