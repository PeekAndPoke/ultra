package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.demo.common.funktorconf.AttendeeModel
import io.peekandpoke.funktor.demo.common.funktorconf.EventModel
import io.peekandpoke.funktor.demo.common.funktorconf.EventStatus
import io.peekandpoke.funktor.demo.common.funktorconf.SaveEventRequest
import io.peekandpoke.funktor.demo.common.funktorconf.SpeakerModel
import io.peekandpoke.funktor.demo.server.api.funktorconf.AttendeeParam
import io.peekandpoke.funktor.demo.server.api.funktorconf.EventParam
import io.peekandpoke.funktor.demo.server.api.funktorconf.FunktorConfApiFeature
import io.peekandpoke.funktor.demo.server.api.funktorconf.SpeakerParam
import io.peekandpoke.funktor.demo.server.funktorconf.Attendee
import io.peekandpoke.funktor.demo.server.funktorconf.AttendeesRepo
import io.peekandpoke.funktor.demo.server.funktorconf.Event
import io.peekandpoke.funktor.demo.server.funktorconf.EventsRepo
import io.peekandpoke.funktor.demo.server.funktorconf.Speaker
import io.peekandpoke.funktor.demo.server.funktorconf.SpeakersRepo
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.ultra.vault.Stored

/**
 * End-to-end coverage of the part-4 migration on the demo conference endpoints (Monko backend):
 * the `{id}` segment now binds a `Stored<Event>` in the param converter instead of a hand-written
 * `findById` in the handler. Asserts:
 * - **Happy path**: public `getEvent` returns a seeded event; a super-user `updateEvent` /
 *   `deleteEvent` succeed.
 * - **Envelope parity**: an unknown id 404s at the binding, exactly as the old `?: notFound()` did.
 * - **Ordering**: anonymous callers on the super-user-floored writes get 401 (the caller-only floor
 *   denies in phase 1, before the entity would be loaded).
 */
class FunktorConfApiTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val api by service(FunktorConfApiFeature::class)
    private val authApi by service(AuthApiFeature::class)
    private val eventsRepo by service(EventsRepo::class)
    private val speakersRepo by service(SpeakersRepo::class)
    private val attendeesRepo by service(AttendeesRepo::class)

    private fun signIn(email: String) = AuthSignInRequest.EmailAndPassword(
        provider = "email-password",
        email = email,
        password = "S3cret123!",
    )

    // A throwaway ref addressing a non-existent key — the typed-route renderer turns it into the url
    // `_key`, and the server-side binding does the real (missing) lookup. Only `_key` is used.
    private fun missingEventRef() =
        Stored(value = Event(name = "gone"), _id = "funktorconf_events/does-not-exist", _key = "does-not-exist")

    private fun missingSpeakerRef() =
        Stored(value = Speaker(name = "gone"), _id = "funktorconf_speakers/does-not-exist", _key = "does-not-exist")

    private fun missingAttendeeRef() =
        Stored(value = Attendee(name = "gone"), _id = "funktorconf_attendees/does-not-exist", _key = "does-not-exist")

    private fun saveRequest(name: String) = SaveEventRequest(
        name = name,
        description = "d",
        venue = "v",
        status = EventStatus.Draft,
        startDate = "2026-01-01",
        endDate = "2026-01-02",
    )

    init {
        installAllFixturesBeforeSpec()

        api.conf.getEvent { getEvent ->
            "public getEvent binds and returns a seeded event (200)" {
                val seeded = eventsRepo.insert(Event(name = "Bound Event"))

                apiApp {
                    anonymous {
                        getEvent(EventParam(id = seeded)) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<EventModel>()!!.name shouldBe "Bound Event"
                        }
                    }
                }
            }

            "public getEvent for an unknown id returns 404 at the binding (envelope parity)" {
                apiApp {
                    anonymous {
                        getEvent(EventParam(id = missingEventRef())) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        // Speaker + Attendee use the identical entity-generic binding as Event; these lock per-group
        // envelope parity AND catch a mis-wired/unregistered repo (a 200 proves the route + repo bind).
        api.conf.getSpeaker { getSpeaker ->
            "public getSpeaker binds and returns a seeded speaker (200)" {
                val seeded = speakersRepo.insert(Speaker(name = "Bound Speaker"))

                apiApp {
                    anonymous {
                        getSpeaker(SpeakerParam(id = seeded)) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<SpeakerModel>()!!.name shouldBe "Bound Speaker"
                        }
                    }
                }
            }

            "public getSpeaker for an unknown id returns 404 at the binding (envelope parity)" {
                apiApp {
                    anonymous {
                        getSpeaker(SpeakerParam(id = missingSpeakerRef())) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        api.conf.getAttendee { getAttendee ->
            "public getAttendee binds and returns a seeded attendee (200)" {
                val seeded = attendeesRepo.insert(Attendee(name = "Bound Attendee"))

                apiApp {
                    anonymous {
                        getAttendee(AttendeeParam(id = seeded)) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AttendeeModel>()!!.name shouldBe "Bound Attendee"
                        }
                    }
                }
            }

            "public getAttendee for an unknown id returns 404 at the binding (envelope parity)" {
                apiApp {
                    anonymous {
                        getAttendee(AttendeeParam(id = missingAttendeeRef())) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        api.confAdmin.updateEvent { updateEvent ->
            val signInRoute = authApi.authLogin.signIn

            "anonymous updateEvent is unauthorized (floor denies before the entity loads)" {
                apiApp {
                    anonymous {
                        updateEvent(EventParam(id = missingEventRef()), saveRequest("X")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "super-user updateEvent binds and updates a seeded event (200)" {
                val seeded = eventsRepo.insert(Event(name = "Before"))

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam(RealmId("admin-user")), body = signIn("karsten.john.gerber@googlemail.com")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .token.token
                        }
                    }

                    authenticate(token) {
                        updateEvent(EventParam(id = seeded), saveRequest("After")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<EventModel>()!!.name shouldBe "After"
                        }
                    }
                }
            }

            "super-user updateEvent for an unknown id returns 404 at the binding (envelope parity)" {
                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam(RealmId("admin-user")), body = signIn("karsten.john.gerber@googlemail.com")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .token.token
                        }
                    }

                    authenticate(token) {
                        updateEvent(EventParam(id = missingEventRef()), saveRequest("Nope")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        api.confAdmin.deleteEvent { deleteEvent ->
            val signInRoute = authApi.authLogin.signIn

            "anonymous deleteEvent is unauthorized" {
                apiApp {
                    anonymous {
                        deleteEvent(EventParam(id = missingEventRef())) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "super-user deleteEvent binds and removes a seeded event (200)" {
                val seeded = eventsRepo.insert(Event(name = "To Delete"))

                apiApp {
                    var token = ""
                    anonymous {
                        signInRoute(RealmParam(RealmId("admin-user")), body = signIn("karsten.john.gerber@googlemail.com")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .token.token
                        }
                    }

                    authenticate(token) {
                        deleteEvent(EventParam(id = seeded)) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }

                // The delete handler removed the bound entity.
                eventsRepo.findById(seeded._key) shouldBe null
            }
        }
    }
}
