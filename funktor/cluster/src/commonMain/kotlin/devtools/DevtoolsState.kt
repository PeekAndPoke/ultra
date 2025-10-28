package de.peekandpoke.funktor.cluster.devtools

import de.peekandpoke.ultra.common.addAt
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.Unsubscribe

object DevtoolsState {
    object RequestHistory : Stream<List<ApiResponse.Insights>> {
        private val stream = StreamSource.Companion<List<ApiResponse.Insights>>(emptyList())

        override fun invoke(): List<ApiResponse.Insights> {
            return stream()
        }

        override fun subscribeToStream(sub: (List<ApiResponse.Insights>) -> Unit): Unsubscribe {
            return stream.subscribeToStream(sub)
        }

        fun add(item: ApiResponse.Insights) {
            stream.modify {
                addAt(0, item).take(100)
            }
        }
    }
}
