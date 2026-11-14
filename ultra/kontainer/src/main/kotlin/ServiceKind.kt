package io.peekandpoke.ultra.kontainer

/**
 * What a service is bound to over the life of a [KontainerBlueprint].
 *
 * Only [Static] and [Dynamic] can be declared; [SemiDynamic] is derived by the blueprint.
 * For a [ServiceType.Singleton] the kind decides how far the instance is shared. For a [ServiceType.Prototype] it
 * changes nothing about when instances are created; it says what the prototype is bound to, which is what makes a
 * singleton above it [SemiDynamic].
 */
enum class ServiceKind {
    /** The same for every kontainer created from the blueprint; a static singleton is shared by all of them */
    Static,

    /** Declared as bound to one kontainer; a dynamic singleton lives once per kontainer */
    Dynamic,

    /** Declared static, but reaches something dynamic through its injections, so it is bound to one kontainer too */
    SemiDynamic,
}
