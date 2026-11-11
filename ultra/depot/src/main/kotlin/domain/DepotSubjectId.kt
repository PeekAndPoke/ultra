package io.peekandpoke.ultra.depot.domain

import kotlin.reflect.KClass

/**
 * Names the data subject of a file: the person (or thing) the data is about. Every write names one.
 *
 * [T] is the kind of subject and is only checked by the compiler: a `DepotRepository<Customer>` takes a
 * `DepotSubjectId<Customer>` and nothing else. The [value], `namespace/id`, is written into the file's metadata.
 *
 * There is no public constructor: an id comes from [of], which names its kind, so the kind is never inferred from
 * what a repository expects.
 */
@JvmInline
value class DepotSubjectId<out T> private constructor(val value: String) {
    companion object {
        /** Files that belong to no person: records of the system itself, technical uploads. No [of] produces it. */
        val system: DepotSubjectId<Nothing> = DepotSubjectId("system")

        /**
         * The subject [id] of kind [type], stored as `namespace/id`. The [namespace] keeps equal ids of different
         * kinds apart (customer 42, employee 42): for a stored entity it is the collection's name. Renaming it makes
         * new files belong to another subject than the old ones.
         *
         * @throws IllegalArgumentException for a blank part, a [namespace] with a `/`, or `Nothing` as the kind
         */
        fun <T : Any> of(type: KClass<T>, namespace: String, id: String): DepotSubjectId<T> {
            require(type != Nothing::class) { "A subject has a kind" }
            require(namespace.isNotBlank() && '/' !in namespace) { "A subject's namespace is not blank and has no '/'" }
            require(id.isNotBlank()) { "A subject's id is not blank" }

            return DepotSubjectId("$namespace/$id")
        }
    }
}
