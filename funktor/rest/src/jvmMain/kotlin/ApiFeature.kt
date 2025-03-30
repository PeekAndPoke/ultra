package de.peekandpoke.ktorfx.rest

/**
 * Common interface for all Api features
 */
interface ApiFeature {
    /** The name of the feature */
    val name: String

    /** The name of the feature used by the code gen */
    val codeGenName: String get() = name

    /** A brief description of the feature */
    val description: String

    /** A list of [ApiRoutes] exposed by the feature */
    fun getRouteGroups(): List<ApiRoutes>
}
