package de.peekandpoke.ultra.kontainer

class LazyImpl<T> internal constructor(provider: () -> T) : Lazy<T> {

    override val value: T by lazy {
        initialized = true
        provider()
    }

    private var initialized = false

    override fun isInitialized(): Boolean = initialized
}
