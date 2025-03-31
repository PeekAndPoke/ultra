package de.peekandpoke.funktor.cluster.locks.api

import de.peekandpoke.funktor.cluster.cluster
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse

class GlobalLocksApi(converter: OutgoingConverter) : ApiRoutes("global-locks", converter) {

    val listServerBeacons = GlobalLocksApiClient.ListServerBeacons.mount {
        docs {
            name = "List server beacons"
        }.codeGen {
            funcName = "listServerBeacons"
        }.authorize {
            isSuperUser()
        }.handle {
            val result = cluster.locks.beacons.list()

            ApiResponse.ok(
                result.map { it.value }
                    .sortedBy { it.serverId }
                    .map {
                        ServerBeaconModel(
                            serverId = it.serverId,
                            serverVersion = it.serverVersion,
                            lastPing = it.lastPing,
                        )
                    }
            )
        }
    }

    val listGlobalLocks = GlobalLocksApiClient.ListGlobalLocks.mount {
        docs {
            name = "List global locks"
        }.codeGen {
            funcName = "listGlobalLocks"
        }.authorize {
            isSuperUser()
        }.handle {

            val result = cluster.locks.global.list()

            ApiResponse.ok(
                result.map {
                    GlobalLockEntryModel(
                        key = it.key,
                        serverId = it.serverId,
                        created = it.created,
                        expires = it.expires,
                    )
                }
            )
        }
    }
}
