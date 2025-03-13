package de.peekandpoke.ultra.kontainer

/**
 * Type of injection
 */
enum class InjectionType {
    /** A singleton service is shared across multiple kontainer instances */
    Singleton,

    /** For a prototype service a new instance is created whenever it is injected */
    Prototype,

    /** A dynamic service is a singleton that only lives within a single kontainer instance */
    Dynamic
}
