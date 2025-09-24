package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlin.time.Duration.Companion.seconds

fun RawCacheDataModel.renderTimestampsAsList(flow: FlowContent) {
    asHead.renderTimestampsAsList(flow)
}

fun RawCacheDataModel.Head.renderTimestampsAsList(flow: FlowContent) {
    val model = this

    with(flow) {
        ui.divided.list {
            noui.item {
                noui.header {
                    +"Expires at"
                }
                noui.content {
                    +model.expiresAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                }
            }
            noui.item {
                noui.header {
                    +"Updated at"
                }
                noui.content {
                    +model.updatedAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                }
            }
            noui.item {
                noui.header {
                    +"Created at"
                }
                noui.content {
                    +model.createdAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                }
            }
        }
    }
}

fun RawCacheDataModel.Head.renderPolicyAsList(flow: FlowContent) {
    val item = this

    with(flow) {
        when (val policy = item.policy) {
            is RawCacheDataModel.Policy.ServeCacheAndRefresh -> {
                ui.divided.list {
                    noui.item {
                        noui.header { +"Serve and refresh" }
                    }
                    noui.item {
                        +"Ttl: ${policy.ttl} sec"
                    }
                    noui.item {
                        +"Refresh after: ${policy.refreshAfter} sec"
                    }
                    noui.item {
                        val nextRefreshIn = policy.refreshAfter - (MpInstant.now() - item.updatedAt).inWholeSeconds

                        +"Next Refresh in: $nextRefreshIn sec"
                    }
                    noui.item {
                        +"Next Refresh at: ${
                            item.updatedAt.atSystemDefaultZone().plus(policy.refreshAfter.seconds)
                                .formatDdMmmYyyyHhMmSs()
                        }"
                    }
                }
            }
        }
    }
}
