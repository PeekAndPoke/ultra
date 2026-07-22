package io.peekandpoke.funktor.demo.server.api.funktorconf

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
import io.peekandpoke.ultra.vault.map

data class FunktorConfIdParams(val id: String)

/**
 * Public conference reads (list / get events, speakers, attendees). Floor: `public()`.
 * The super-user writes live in the separate [FunktorConfAdminApi] group.
 */
class FunktorConfApi(
    private val services: FunktorConfServices,
) : ApiRoutes("funktor-conf", defaultAuth = { public() }) {

    val listEvents = FunktorConfApiClient.ListEvents.mount {
        docs {
            name = "List all events"
        }.codeGen {
            funcName = "listEvents"
        }.handle {
            val events = services.eventsRepo.findAll().map { it.asApiModel() }
            ApiResponse.ok(events)
        }
    }

    val getEvent = FunktorConfApiClient.GetEvent.mount(FunktorConfIdParams::class) {
        docs {
            name = "Get event by ID"
        }.codeGen {
            funcName = "getEvent"
        }.handle { params ->
            val event = services.eventsRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            ApiResponse.ok(event.asApiModel())
        }
    }

    val listSpeakers = FunktorConfApiClient.ListSpeakers.mount {
        docs {
            name = "List all speakers"
        }.codeGen {
            funcName = "listSpeakers"
        }.handle {
            val speakers = services.speakersRepo.findAll().map { it.asApiModel() }
            ApiResponse.ok(speakers)
        }
    }

    val getSpeaker = FunktorConfApiClient.GetSpeaker.mount(FunktorConfIdParams::class) {
        docs {
            name = "Get speaker by ID"
        }.codeGen {
            funcName = "getSpeaker"
        }.handle { params ->
            val speaker = services.speakersRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            ApiResponse.ok(speaker.asApiModel())
        }
    }

    val listAttendees = FunktorConfApiClient.ListAttendees.mount {
        docs {
            name = "List all attendees"
        }.codeGen {
            funcName = "listAttendees"
        }.handle {
            val attendees = services.attendeesRepo.findAll().toList().map { it.asApiModel() }
            ApiResponse.ok(attendees)
        }
    }

    val getAttendee = FunktorConfApiClient.GetAttendee.mount(FunktorConfIdParams::class) {
        docs {
            name = "Get attendee by ID"
        }.codeGen {
            funcName = "getAttendee"
        }.handle { params ->
            val attendee = services.attendeesRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            ApiResponse.ok(attendee.asApiModel())
        }
    }
}

/**
 * Super-user conference writes (create / update / delete events, speakers, attendees).
 * Floor: `isSuperUser()`.
 */
class FunktorConfAdminApi(
    private val services: FunktorConfServices,
) : ApiRoutes("funktor-conf-admin", defaultAuth = { isSuperUser() }) {

    val createEvent = FunktorConfApiClient.CreateEvent.mount {
        docs {
            name = "Create a new event"
        }.codeGen {
            funcName = "createEvent"
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

    val updateEvent = FunktorConfApiClient.UpdateEvent.mount(FunktorConfIdParams::class) {
        docs {
            name = "Update an event"
        }.codeGen {
            funcName = "updateEvent"
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

    val deleteEvent = FunktorConfApiClient.DeleteEvent.mount(FunktorConfIdParams::class) {
        docs {
            name = "Delete an event"
        }.codeGen {
            funcName = "deleteEvent"
        }.handle { params ->
            val existing = services.eventsRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            services.eventsRepo.remove(existing)
            ApiResponse.ok(existing.asApiModel())
        }
    }

    val createSpeaker = FunktorConfApiClient.CreateSpeaker.mount {
        docs {
            name = "Create a new speaker"
        }.codeGen {
            funcName = "createSpeaker"
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

    val updateSpeaker = FunktorConfApiClient.UpdateSpeaker.mount(FunktorConfIdParams::class) {
        docs {
            name = "Update a speaker"
        }.codeGen {
            funcName = "updateSpeaker"
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

    val deleteSpeaker = FunktorConfApiClient.DeleteSpeaker.mount(FunktorConfIdParams::class) {
        docs {
            name = "Delete a speaker"
        }.codeGen {
            funcName = "deleteSpeaker"
        }.handle { params ->
            val existing = services.speakersRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            services.speakersRepo.remove(existing)
            ApiResponse.ok(existing.asApiModel())
        }
    }

    val createAttendee = FunktorConfApiClient.CreateAttendee.mount {
        docs {
            name = "Create a new attendee"
        }.codeGen {
            funcName = "createAttendee"
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

    val updateAttendee = FunktorConfApiClient.UpdateAttendee.mount(FunktorConfIdParams::class) {
        docs {
            name = "Update an attendee"
        }.codeGen {
            funcName = "updateAttendee"
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

    val deleteAttendee = FunktorConfApiClient.DeleteAttendee.mount(FunktorConfIdParams::class) {
        docs {
            name = "Delete an attendee"
        }.codeGen {
            funcName = "deleteAttendee"
        }.handle { params ->
            val existing = services.attendeesRepo.findById(params.id)
                ?: return@handle ApiResponse.notFound()

            services.attendeesRepo.remove(existing)
            ApiResponse.ok(existing.asApiModel())
        }
    }
}
