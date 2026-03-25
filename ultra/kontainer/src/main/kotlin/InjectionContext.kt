package de.peekandpoke.ultra.kontainer

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * The injection context as passed through to all parameter providers.
 *
 * It carries
 * - the current [kontainer] instances
 * - the service that is injecting: [injectingClass]
 * - the service that is requesting a service: [requestingClass]
 */
class InjectionContext internal constructor(
    /** Class requesting the service */
    val requestingClass: KClass<out Any>,
    /** Class that is about to be injected */
    val injectingClass: KClass<out Any>,
) {
    companion object {
        internal val kontainerRoot = InjectionContext(Kontainer::class, Kontainer::class)
    }

    private val nextCache = ConcurrentHashMap<KClass<*>, InjectionContext>()

    /**
     * Advances the context.
     *
     * Makes the [injectingClass] the [requestingClass]
     * Makes the passed in [nextInjectingClass] the [injectingClass]
     */
    internal fun next(nextInjectingClass: KClass<out Any>): InjectionContext {
        return nextCache.getOrPut(nextInjectingClass) {
            InjectionContext(
                requestingClass = injectingClass,
                injectingClass = nextInjectingClass
            )
        }
    }
}
