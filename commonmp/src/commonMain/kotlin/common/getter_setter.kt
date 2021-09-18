package de.peekandpoke.ultra.common

interface GetAndSet<P> {
    operator fun invoke(): P

    operator fun invoke(input: P): P
}

