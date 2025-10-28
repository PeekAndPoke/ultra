package de.peekandpoke.funktor.logging.api

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class LogsRequest {

    @Serializable
    data class BulkResponse(
        val numChanged: Int,
    )

    @Serializable
    data class BulkAction(
        val filter: Filter,
        val action: Action,
    ) {
        @Serializable
        data class Filter(
            val from: MpInstant?,
            val to: MpInstant?,
            val states: Set<LogEntryModel.State>? = emptySet(),
        )
    }

    @Serializable
    sealed class Action {
        @Serializable
        @SerialName("set-state")
        data class SetState(val state: LogEntryModel.State) : Action()
    }
}
