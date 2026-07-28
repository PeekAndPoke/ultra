package io.peekandpoke.ultra.vault.tools

/** Bundles the vault's database tooling into one injectable service. */
class DatabaseTools(
    /** Provides the database graph via [DatabaseGraphBuilder.getGraph]. */
    val graphBuilder: DatabaseGraphBuilder,
)
