package io.peekandpoke.funktor.demo.server.funktorconf

import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.demo.common.funktorconf.AttendeeModel
import io.peekandpoke.funktor.demo.common.funktorconf.TicketType
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class AttendeesRepo(
    driver: MonkoDriver,
    timestamped: TimestampedHook,
) : MonkoRepository<Attendee>(
    name = "funktorconf_attendees",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(emptyList<Repository.Hooks.OnAfterSave<Attendee>>())
        .plus(timestamped.onBeforeSave())
) {
    companion object {
        fun Storable<Attendee>.asApiModel() = with(value) {
            AttendeeModel(
                id = _id,
                name = name,
                email = email,
                ticketType = ticketType,
                checkedIn = checkedIn,
                createdAt = createdAt.toIsoString(),
                updatedAt = updatedAt.toIsoString(),
            )
        }
    }

    @Suppress("unused", "CanBeParameter")
    class Fixtures(
        repo: AttendeesRepo,
        private val events: EventsRepo.Fixtures,
        private val speakers: SpeakersRepo.Fixtures,
    ) : RepoFixtureLoader<Attendee>(repo = repo) {

        override val dependsOn: List<FixtureLoader> = listOf(events, speakers)

        // VIP attendees — ecosystem sponsors and partners

        val vipSponsor = fix {
            "petra-lindqvist" to Attendee(
                name = "Petra Lindqvist",
                email = "petra.lindqvist@example.com",
                ticketType = TicketType.Vip,
                checkedIn = true,
            )
        }

        val vipPartner = fix {
            "oliver-klein" to Attendee(
                name = "Oliver Klein",
                email = "oliver.klein@example.com",
                ticketType = TicketType.Vip,
                checkedIn = true,
            )
        }

        // Speaker tickets — linked to speaker fixtures

        val elenaAsAttendee = fix {
            key(speakers.elenaBrandt) to Attendee(
                name = "Elena Brandt",
                email = "elena.brandt@example.com",
                ticketType = TicketType.Speaker,
                checkedIn = true,
            )
        }

        val tomaszAsAttendee = fix {
            key(speakers.tomaszWielki) to Attendee(
                name = "Tomasz Wielki",
                email = "tomasz.wielki@example.com",
                ticketType = TicketType.Speaker,
                checkedIn = true,
            )
        }

        val raviAsAttendee = fix {
            key(speakers.raviPatel) to Attendee(
                name = "Ravi Patel",
                email = "ravi.patel@example.com",
                ticketType = TicketType.Speaker,
                checkedIn = false,
            )
        }

        // Staff

        val staffOrganizer = fix {
            "maja-stein" to Attendee(
                name = "Maja Stein",
                email = "maja.stein@example.com",
                ticketType = TicketType.Staff,
                checkedIn = true,
            )
        }

        val staffVolunteer = fix {
            "lukas-frey" to Attendee(
                name = "Lukas Frey",
                email = "lukas.frey@example.com",
                ticketType = TicketType.Staff,
                checkedIn = false,
            )
        }

        // Standard attendees — regular conference-goers

        val standardAttendees: List<Fix<Attendee>> = listOf(
            "Anja Meier" to "anja.meier@example.com",
            "Ben Hartmann" to "ben.hartmann@example.com",
            "Chiara Rossi" to "chiara.rossi@example.com",
            "Daan Bakker" to "daan.bakker@example.com",
            "Emilia Nowak" to "emilia.nowak@example.com",
            "Finn Andersen" to "finn.andersen@example.com",
            "Giulia Bianchi" to "giulia.bianchi@example.com",
            "Hiro Nakamura" to "hiro.nakamura@example.com",
            "Ida Larsson" to "ida.larsson@example.com",
            "Jonas Weber" to "jonas.weber@example.com",
            "Katrin Huber" to "katrin.huber@example.com",
            "Leo Fontaine" to "leo.fontaine@example.com",
        ).mapIndexed { idx, (name, email) ->
            fix {
                val key = name.lowercase().replace(" ", "-")
                key to Attendee(
                    name = name,
                    email = email,
                    ticketType = TicketType.Standard,
                    checkedIn = idx < 8,
                )
            }
        }
    }

    override fun MonkoIndexBuilder<Attendee>.buildIndexes() {
        persistentIndex {
            field { it.email }
        }

        persistentIndex {
            field { it.ticketType }
        }
    }
}
