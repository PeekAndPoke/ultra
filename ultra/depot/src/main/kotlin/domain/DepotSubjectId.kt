package io.peekandpoke.ultra.depot.domain

/**
 * Names the data subject of a file: the person (or thing) the data is about. Every write names one.
 *
 * [T] is the kind of subject and is only checked by the compiler: a `DepotRepository<Customer>` takes a
 * `DepotSubjectId<Customer>` and nothing else. The id is written into the file's metadata.
 */
@JvmInline
value class DepotSubjectId<out T>(val value: String) {
    companion object {
        /** Files that belong to no person: records of the system itself, technical uploads. */
        val system: DepotSubjectId<Nothing> = DepotSubjectId("system")
    }
}
