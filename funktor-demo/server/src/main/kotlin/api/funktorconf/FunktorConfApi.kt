package io.peekandpoke.funktor.demo.server.api.funktorconf

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.demo.common.funktorconf.FunktorConfApiClient
import io.peekandpoke.funktor.demo.server.funktorconf.Attendee
import io.peekandpoke.funktor.demo.server.funktorconf.AttendeesRepo.Companion.asApiModel
import io.peekandpoke.funktor.demo.server.funktorconf.Event
import io.peekandpoke.funktor.demo.server.funktorconf.EventsRepo.Companion.asApiModel
import io.peekandpoke.funktor.demo.server.funktorconf.FunktorConfServices
import io.peekandpoke.funktor.demo.server.funktorconf.Speaker
import io.peekandpoke.funktor.demo.server.funktorconf.SpeakersRepo.Companion.asApiModel
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.vault.New

class FunktorConfApi(
    converter: OutgoingConverter,
    private val services: FunktorConfServices,
) : ApiRoutes("funktor-conf", converter) {

    data class IdParams(val id: String)

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Events
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val listEvents = FunktorConfApiClient.ListEvents.mount {
        docs {
            name = "List all events"
        }.codeGen {
            funcName = "listEvents"
        }.authorize {
            public()
        }.handle {
            val events = services.eventsRepo.findAll().toList().map { it.asApiModel() }
            ApiResponse.ok(events)
        }
    }

    val getEvent = FunktorConfApiClient.GetEvent.mount(IdParams::class) {
        docs {
            name = "Get event by ID"
        }.codeGen {
            funcName = "getEvent"
        }.authorize {
            public()
        }.handle { params ->
            val event = services.eventsRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            ApiResponse.ok(event.asApiModel())
        }
    }

    val createEvent = FunktorConfApiClient.CreateEvent.mount {
        docs {
            name = "Create a new event"
        }.codeGen {
            funcName = "createEvent"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val event = services.eventsRepo.insert(
                New(
                    value = Event(
                        name = body.name,
                        description = body.description,
                        venue = body.venue,
                        status = body.status,
                        startDate = body.startDate,
                        endDate = body.endDate,
                    )
                )
            )
            ApiResponse.ok(event.asApiModel())
        }
    }

    val updateEvent = FunktorConfApiClient.UpdateEvent.mount(IdParams::class) {
        docs {
            name = "Update an event"
        }.codeGen {
            funcName = "updateEvent"
        }.authorize {
            isSuperUser()
        }.handle { params, body ->
            val existing = services.eventsRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            val updated = services.eventsRepo.save(
                existing.modify {
                    it.copy(
                        name = body.name,
                        description = body.description,
                        venue = body.venue,
                        status = body.status,
                        startDate = body.startDate,
                        endDate = body.endDate,
                    )
                }
            )
            ApiResponse.ok(updated.asApiModel())
        }
    }

    val deleteEvent = FunktorConfApiClient.DeleteEvent.mount(IdParams::class) {
        docs {
            name = "Delete an event"
        }.codeGen {
            funcName = "deleteEvent"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val existing = services.eventsRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            services.eventsRepo.remove(existing)
            ApiResponse.ok(existing.asApiModel())
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Speakers
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val listSpeakers = FunktorConfApiClient.ListSpeakers.mount {
        docs {
            name = "List all speakers"
        }.codeGen {
            funcName = "listSpeakers"
        }.authorize {
            public()
        }.handle {
            val speakers = services.speakersRepo.findAll().toList().map { it.asApiModel() }
            ApiResponse.ok(speakers)
        }
    }

    val getSpeaker = FunktorConfApiClient.GetSpeaker.mount(IdParams::class) {
        docs {
            name = "Get speaker by ID"
        }.codeGen {
            funcName = "getSpeaker"
        }.authorize {
            public()
        }.handle { params ->
            val speaker = services.speakersRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            ApiResponse.ok(speaker.asApiModel())
        }
    }

    val createSpeaker = FunktorConfApiClient.CreateSpeaker.mount {
        docs {
            name = "Create a new speaker"
        }.codeGen {
            funcName = "createSpeaker"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val speaker = services.speakersRepo.insert(
                New(
                    value = Speaker(
                        name = body.name,
                        bio = body.bio,
                        photoUrl = body.photoUrl,
                        talkTitle = body.talkTitle,
                        talkAbstract = body.talkAbstract,
                    )
                )
            )
            ApiResponse.ok(speaker.asApiModel())
        }
    }

    val updateSpeaker = FunktorConfApiClient.UpdateSpeaker.mount(IdParams::class) {
        docs {
            name = "Update a speaker"
        }.codeGen {
            funcName = "updateSpeaker"
        }.authorize {
            isSuperUser()
        }.handle { params, body ->
            val existing = services.speakersRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            val updated = services.speakersRepo.save(
                existing.modify {
                    it.copy(
                        name = body.name,
                        bio = body.bio,
                        photoUrl = body.photoUrl,
                        talkTitle = body.talkTitle,
                        talkAbstract = body.talkAbstract,
                    )
                }
            )
            ApiResponse.ok(updated.asApiModel())
        }
    }

    val deleteSpeaker = FunktorConfApiClient.DeleteSpeaker.mount(IdParams::class) {
        docs {
            name = "Delete a speaker"
        }.codeGen {
            funcName = "deleteSpeaker"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val existing = services.speakersRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            services.speakersRepo.remove(existing)
            ApiResponse.ok(existing.asApiModel())
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Attendees
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val listAttendees = FunktorConfApiClient.ListAttendees.mount {
        docs {
            name = "List all attendees"
        }.codeGen {
            funcName = "listAttendees"
        }.authorize {
            public()
        }.handle {
            val attendees = services.attendeesRepo.findAll().toList().map { it.asApiModel() }
            ApiResponse.ok(attendees)
        }
    }

    val getAttendee = FunktorConfApiClient.GetAttendee.mount(IdParams::class) {
        docs {
            name = "Get attendee by ID"
        }.codeGen {
            funcName = "getAttendee"
        }.authorize {
            public()
        }.handle { params ->
            val attendee = services.attendeesRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            ApiResponse.ok(attendee.asApiModel())
        }
    }

    val createAttendee = FunktorConfApiClient.CreateAttendee.mount {
        docs {
            name = "Create a new attendee"
        }.codeGen {
            funcName = "createAttendee"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val attendee = services.attendeesRepo.insert(
                New(
                    value = Attendee(
                        name = body.name,
                        email = body.email,
                        ticketType = body.ticketType,
                        checkedIn = body.checkedIn,
                    )
                )
            )
            ApiResponse.ok(attendee.asApiModel())
        }
    }

    val updateAttendee = FunktorConfApiClient.UpdateAttendee.mount(IdParams::class) {
        docs {
            name = "Update an attendee"
        }.codeGen {
            funcName = "updateAttendee"
        }.authorize {
            isSuperUser()
        }.handle { params, body ->
            val existing = services.attendeesRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            val updated = services.attendeesRepo.save(
                existing.modify {
                    it.copy(
                        name = body.name,
                        email = body.email,
                        ticketType = body.ticketType,
                        checkedIn = body.checkedIn,
                    )
                }
            )
            ApiResponse.ok(updated.asApiModel())
        }
    }

    val deleteAttendee = FunktorConfApiClient.DeleteAttendee.mount(IdParams::class) {
        docs {
            name = "Delete an attendee"
        }.codeGen {
            funcName = "deleteAttendee"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val existing = services.attendeesRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            services.attendeesRepo.remove(existing)
            ApiResponse.ok(existing.asApiModel())
        }
    }
}
