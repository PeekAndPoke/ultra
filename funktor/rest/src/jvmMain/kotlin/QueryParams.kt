package de.peekandpoke.funktor.rest

/**
 * Helper classes for query params and query bodies
 */
interface QueryParams {

    /** Query Params for searching a list of entities */
    data class List(
        val search: String = "",
        val page: Int = 1,
        val epp: Int = 50,
    )

    /** Query Params for getting an entity by id */
    data class GetById(
        val id: String,
    )
}
