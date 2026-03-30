package io.peekandpoke.funktor.demo.common.funktorconf

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

class FunktorConfApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/funktor-conf"

        // Event endpoints
        val ListEvents = TypedApiEndpoint.Get(
            uri = "$BASE/events",
            response = EventModel.serializer().apiList(),
        )

        val GetEvent = TypedApiEndpoint.Get(
            uri = "$BASE/events/{id}",
            response = EventModel.serializer().api(),
        )

        val CreateEvent = TypedApiEndpoint.Post(
            uri = "$BASE/events",
            body = SaveEventRequest.serializer(),
            response = EventModel.serializer().api(),
        )

        val UpdateEvent = TypedApiEndpoint.Put(
            uri = "$BASE/events/{id}",
            body = SaveEventRequest.serializer(),
            response = EventModel.serializer().api(),
        )

        val DeleteEvent = TypedApiEndpoint.Delete(
            uri = "$BASE/events/{id}",
            response = EventModel.serializer().api(),
        )

        // Speaker endpoints
        val ListSpeakers = TypedApiEndpoint.Get(
            uri = "$BASE/speakers",
            response = SpeakerModel.serializer().apiList(),
        )

        val GetSpeaker = TypedApiEndpoint.Get(
            uri = "$BASE/speakers/{id}",
            response = SpeakerModel.serializer().api(),
        )

        val CreateSpeaker = TypedApiEndpoint.Post(
            uri = "$BASE/speakers",
            body = SaveSpeakerRequest.serializer(),
            response = SpeakerModel.serializer().api(),
        )

        val UpdateSpeaker = TypedApiEndpoint.Put(
            uri = "$BASE/speakers/{id}",
            body = SaveSpeakerRequest.serializer(),
            response = SpeakerModel.serializer().api(),
        )

        val DeleteSpeaker = TypedApiEndpoint.Delete(
            uri = "$BASE/speakers/{id}",
            response = SpeakerModel.serializer().api(),
        )

        // Attendee endpoints
        val ListAttendees = TypedApiEndpoint.Get(
            uri = "$BASE/attendees",
            response = AttendeeModel.serializer().apiList(),
        )

        val GetAttendee = TypedApiEndpoint.Get(
            uri = "$BASE/attendees/{id}",
            response = AttendeeModel.serializer().api(),
        )

        val CreateAttendee = TypedApiEndpoint.Post(
            uri = "$BASE/attendees",
            body = SaveAttendeeRequest.serializer(),
            response = AttendeeModel.serializer().api(),
        )

        val UpdateAttendee = TypedApiEndpoint.Put(
            uri = "$BASE/attendees/{id}",
            body = SaveAttendeeRequest.serializer(),
            response = AttendeeModel.serializer().api(),
        )

        val DeleteAttendee = TypedApiEndpoint.Delete(
            uri = "$BASE/attendees/{id}",
            response = AttendeeModel.serializer().api(),
        )
    }

    // Event calls

    fun listEvents(): Flow<ApiResponse<List<EventModel>>> = call(ListEvents())

    fun getEvent(id: String): Flow<ApiResponse<EventModel>> = call(GetEvent("id" to id))

    fun createEvent(request: SaveEventRequest): Flow<ApiResponse<EventModel>> = call(
        CreateEvent(body = request)
    )

    fun updateEvent(id: String, request: SaveEventRequest): Flow<ApiResponse<EventModel>> = call(
        UpdateEvent("id" to id, body = request)
    )

    fun deleteEvent(id: String): Flow<ApiResponse<EventModel>> = call(DeleteEvent("id" to id))

    // Speaker calls

    fun listSpeakers(): Flow<ApiResponse<List<SpeakerModel>>> = call(ListSpeakers())

    fun getSpeaker(id: String): Flow<ApiResponse<SpeakerModel>> = call(GetSpeaker("id" to id))

    fun createSpeaker(request: SaveSpeakerRequest): Flow<ApiResponse<SpeakerModel>> = call(
        CreateSpeaker(body = request)
    )

    fun updateSpeaker(id: String, request: SaveSpeakerRequest): Flow<ApiResponse<SpeakerModel>> = call(
        UpdateSpeaker("id" to id, body = request)
    )

    fun deleteSpeaker(id: String): Flow<ApiResponse<SpeakerModel>> = call(DeleteSpeaker("id" to id))

    // Attendee calls

    fun listAttendees(): Flow<ApiResponse<List<AttendeeModel>>> = call(ListAttendees())

    fun getAttendee(id: String): Flow<ApiResponse<AttendeeModel>> = call(GetAttendee("id" to id))

    fun createAttendee(request: SaveAttendeeRequest): Flow<ApiResponse<AttendeeModel>> = call(
        CreateAttendee(body = request)
    )

    fun updateAttendee(id: String, request: SaveAttendeeRequest): Flow<ApiResponse<AttendeeModel>> = call(
        UpdateAttendee("id" to id, body = request)
    )

    fun deleteAttendee(id: String): Flow<ApiResponse<AttendeeModel>> = call(DeleteAttendee("id" to id))
}
