package io.peekandpoke.funktor.insights.api

import kotlinx.serialization.Serializable

/**
 * The service definition graph of the app's kontainer, as [InsightsApi.GetKontainerGraph] serves it.
 *
 * Static per blueprint, which is why a record does not carry it: a record's `kontainer` slice lists
 * only the services that had instances, keyed by [Service.cls], plus the [fingerprint] of the graph it
 * was written against. Enum-valued fields are plain strings, so a value added later passes through.
 */
@Serializable
data class InsightsKontainerGraph(
    /** SHA-256 hex of [services]. A record whose `graphFingerprint` differs was written against another graph. */
    val fingerprint: String,
    /** Every service the blueprint defines, ordered by [Service.cls]. */
    val services: List<Service>,
) {
    /** A service as the blueprint defines it. */
    @Serializable
    data class Service(
        /** The FQN the service is registered under; a record's slice joins on it. */
        val cls: String,
        /** `Singleton`, `SemiDynamic`, `Dynamic` or `Prototype` — never `DynamicOverride`, which only a request's kontainer reports. */
        val type: String,
        val definition: Definition,
    )

    /** What a service's producer creates and injects. */
    @Serializable
    data class Definition(
        /** FQN of the class the producer creates. */
        val creates: String,
        /**
         * `Singleton`, `Dynamic` or `Prototype`, as registered — upgraded to `Dynamic` or `Prototype` when it
         * replaces a definition of that type. The downgrade to `SemiDynamic` shows only in [Service.type].
         */
        val injectionType: String,
        /** In constructor order. */
        val injects: List<Injection>,
        /** The stack frame that registered the definition. */
        val codeLocation: String,
        /** The definition this one replaced; null at the end of the chain. */
        val overwrites: Definition?,
    )

    /** One constructor parameter of a [Definition]. */
    @Serializable
    data class Injection(
        val name: String,
        /** `Direct` or `Lazy`. */
        val provisionType: String,
        /**
         * Sorted FQNs of what the parameter asks for: its DECLARED type for a single service — often an
         * interface that is no service key, e.g. `AppConfig` — or every candidate for a `List` or `Lookup`.
         */
        val classes: List<String>,
    )
}

/**
 * The repositories and the references between the classes they store, as [InsightsApi.GetDatabaseGraph]
 * serves it.
 *
 * `DatabaseGraphModel` without its two dead fields: `Repo.connection` duplicated [Repo.Id.connection], and
 * `Reference.type` is always `Direct` since `LazyRef` was merged into `Ref`.
 */
@Serializable
data class InsightsDatabaseGraph(
    val repos: List<Repo>,
) {
    /** One repository and the classes it stores. */
    @Serializable
    data class Repo(
        val id: Id,
        val storedClasses: List<StoredClass>,
    ) {
        /** A repository's name and connection, e.g. `ArangoDB(3.12.4)::my_db`. */
        @Serializable
        data class Id(
            val name: String,
            val connection: String,
        )
    }

    /** A class a repository stores, with the references found in its constructor graph. */
    @Serializable
    data class StoredClass(
        val fqn: String,
        val references: List<Reference>,
    )

    /** A reference from a stored class to another entity type. */
    @Serializable
    data class Reference(
        /** The repository storing the referenced type; null when no registered repository stores it. */
        val repo: Repo.Id?,
        val fqn: String,
    )
}
