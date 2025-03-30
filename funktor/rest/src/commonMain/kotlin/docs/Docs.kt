package de.peekandpoke.ktorfx.rest.docs

annotation class Docs {
    annotation class Description(
        val description: String,
    )

    annotation class DefaultValue(
        val value: String,
    )

    annotation class Deprecated(
        val inVersion: String,
        val comment: String = "",
    )
}
