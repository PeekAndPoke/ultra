package io.peekandpoke.ultra.depot.domain

import kotlin.reflect.KClass

/**
 * Names the data subject of a file: the person (or thing) the data is about. Every write names one.
 *
 * [T] is the kind of subject and is only checked by the compiler: a `DepotRepository<Customer>` takes a
 * `DepotSubjectId<Customer>` (or [system]), not another kind's. The [value], `namespace/id`, is written into the file's metadata.
 *
 * There is no public constructor: an id comes from [of], which names its kind, so the kind is never inferred from
 * what a repository expects.
 */
@JvmInline
value class DepotSubjectId<out T> private constructor(val value: String) {
    companion object {
        /**
         * Files not attributed to one person: records of the system itself, technical uploads. No [of] produces it.
         * It says nothing about whether a file holds personal data.
         */
        val system: DepotSubjectId<Nothing> = DepotSubjectId("system")

        /**
         * The subject [id] of kind [type], stored as `namespace/id`. The [namespace] keeps equal ids of different
         * kinds apart (customer 42, employee 42): for a stored entity it is the collection's name, and a stored
         * entity's id is made by its `asDepotSubject()` (funktor cluster), never by hand with another spelling. Another
         * namespace is another subject: renaming it makes new files belong to another subject than the old ones.
         *
         * @throws IllegalArgumentException for a blank part, a [namespace] with a `/`, text that is not valid Unicode, or `Nothing`
         * as the kind
         */
        fun <T : Any> of(type: KClass<T>, namespace: String, id: String): DepotSubjectId<T> {
            require(type != Nothing::class) { "A subject has a kind" }
            require(namespace.isNotBlank() && '/' !in namespace) { "A subject's namespace is not blank and has no '/'" }
            require(id.isNotBlank()) { "A subject's id is not blank" }
            // An unpaired surrogate would be stored as '?', and two ids would become one
            require("$namespace/$id".let { it.encodeToByteArray().decodeToString() == it }) { "A subject's id is valid text" }

            return DepotSubjectId("$namespace/$id")
        }
    }
}
