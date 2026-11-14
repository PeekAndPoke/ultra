package io.peekandpoke.ultra.kontainer

/**
 * What a service is: one shared instance, or a new instance per injection.
 *
 * The second question, what the service is bound to, is its [ServiceKind].
 */
enum class ServiceType {
    /** One instance, shared by everyone who injects it (within its [ServiceKind]'s scope) */
    Singleton,

    /** A new instance for every injection and every get */
    Prototype,
}
