package de.peekandpoke.ultra.streams

/**
 * Define a storage backend for a persistent stream
 */
interface StreamStorage<T> {

    fun read(): T?

    fun write(value: T)
}
