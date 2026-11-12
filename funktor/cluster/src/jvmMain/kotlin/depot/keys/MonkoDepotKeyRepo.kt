package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.ultra.reflection.kType

/** The depot's subject keys in MongoDB. Its queries are profiled without their values. */
class MonkoDepotKeyRepo(driver: MonkoDriver, repoName: String) :
    MonkoRepository<DepotSubjectKeyRecord>(name = repoName, kType(), driver.withholdingQueryValues()), VaultDepotKeyStore.Repo
