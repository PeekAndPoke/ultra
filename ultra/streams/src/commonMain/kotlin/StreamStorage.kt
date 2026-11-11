package io.peekandpoke.ultra.streams

/**
 * Define a storage backend for a persistent stream
 */
interface StreamStorage<T> {

    /** A value as stored: `Stored(null)` is a stored `null`, unlike "nothing stored" (ULTRA-00309). */
    data class Stored<T>(val value: T)

    fun read(): T?

    /**
     * What is stored, or null when nothing is. Override it when [T] is nullable and a `null` may be stored: the default
     * reads a stored `null` as nothing stored.
     */
    fun readStored(): Stored<T>? = read()?.let { Stored(it) }

    fun write(value: T)
}
