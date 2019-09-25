package de.peekandpoke.ultra.common

class SimpleLazy<T>(provider: () -> T) : Lazy<T> {

    override val value: T by lazy {
        initialized = true
        provider()
    }

    private var initialized = false

    override fun isInitialized(): Boolean = initialized
}
