package de.peekandpoke.ultra.kontainer

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

    private val nextCache = mutableMapOf<KClass<*>, InjectionContext>()

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

//    /**
//     * Gets a config parameter from the kontainer
//     */
//    internal fun <T> getConfig(name: String): T = kontainer.getConfig(name)
//
//    /**
//     * Gets a service from the kontainer
//     */
//    internal fun <T : Any> get(cls: KClass<T>): T = kontainer.get(cls, this)
//
//    /**
//     * Gets a service or null from the kontainer
//     */
//    internal fun <T : Any> getOrNull(cls: KClass<T>): T? = kontainer.getOrNull(cls, this)
//
//    /**
//     * Gets all supertype services for the given [cls] as a List
//     */
//    internal fun <T : Any> getAll(cls: KClass<T>): List<T> = kontainer.getAll(cls, this)
//
//    /**
//     * Gets all supertype services for the given [cls] as a Lookup
//     */
//    internal fun <T : Any> getLookup(cls: KClass<T>): LazyServiceLookup<T> = kontainer.getLookup(cls, this)
}
