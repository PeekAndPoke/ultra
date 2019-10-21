package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

/**
 * The injection context as passed through to all parameter providers.
 *
 * It carries
 * - the current [kontainer] instances
 * - the service that is currently created: [currentClass]
 * - the service that is requesting the currently created service: [requestingClass]
 */
data class InjectionContext(
    /** The container instance */
    val kontainer: Kontainer,
    /** Class requesting the service */
    val requestingClass: KClass<out Any>,
    /** Class that is about to be injected */
    val currentClass: KClass<out Any>
) {
    /**
     * Advances the context.
     *
     * Makes the [currentClass] the [requestingClass]
     * Makes the passed in [cls] the [currentClass]
     */
    internal fun next(cls: KClass<out Any>) = copy(
        requestingClass = currentClass,
        currentClass = cls
    )

    /**
     * Gets a config parameter from the kontainer
     */
    internal fun <T> getConfig(name: String): T = kontainer.getConfig(name)

    /**
     * Gets a service from the kontainer
     */
    internal fun <T : Any> get(cls: KClass<T>): T = kontainer.get(cls, this)

    /**
     * Gets a service or null from the kontainer
     */
    internal fun <T : Any> getOrNull(cls: KClass<T>): T? = kontainer.getOrNull(cls, this)

    /**
     * Gets all supertype services for the given [cls] as a List
     */
    internal fun <T : Any> getAll(cls: KClass<T>): List<T> = kontainer.getAll(cls, this)

    /**
     * Gets all supertype services for the given [cls] as a Lookup
     */
    internal fun <T : Any> getLookup(cls: KClass<T>): Lookup<T> = kontainer.getLookup(cls, this)
}
