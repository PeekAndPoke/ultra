package io.peekandpoke.funktor.insights

import io.peekandpoke.ultra.depot.repos.DepotRepository

/** Depot repository marker for storing insights data; its records belong to the system, not to a person. */
interface InsightsRepository : DepotRepository<Any>
