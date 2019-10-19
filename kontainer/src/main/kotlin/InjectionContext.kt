package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

data class InjectionContext(
    /**
     * The container instance
     */
    val kontainer: Kontainer,
    /**
     * Class requesting the service
     */
    val requestingClass: KClass<out Any>,
    /**
     * Class that is about to be injected
     */
    val currentClass: KClass<out Any>
) {
    internal fun next(cls: KClass<out Any>) = copy(
        requestingClass = currentClass,
        currentClass = cls
    )

    internal fun <T> getConfig(name: String) = kontainer.getConfig<T>(name)

    internal fun <T : Any> get(cls: KClass<T>) = kontainer.get(cls, this)

    internal fun <T : Any> getOrNull(cls: KClass<T>) = kontainer.getOrNull(cls, this)

    internal fun <T : Any> getAll(cls: KClass<T>) = kontainer.getAll(cls, this)

    internal fun <T : Any> getLookup(cls: KClass<T>) = kontainer.getLookup(cls, this)
}
