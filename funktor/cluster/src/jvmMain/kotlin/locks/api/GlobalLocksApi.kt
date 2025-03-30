package de.peekandpoke.ktorfx.cluster.locks.api

import de.peekandpoke.ktorfx.cluster.cluster
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
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
