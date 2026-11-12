package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.reflection.kType

/** The depot's subject keys in ArangoDB. Its queries are profiled without their values. */
class KarangoDepotKeyRepo(driver: KarangoDriver, repoName: String) :
    EntityRepository<DepotSubjectKeyRecord>(name = repoName, kType(), driver.withholdingQueryValues()), VaultDepotKeyStore.Repo
